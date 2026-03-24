"""
services/google_maps.py — Wrapper around Google Maps Platform APIs.

Handles:
  - Directions API (route + turn-by-turn)
  - Distance Matrix API (multi-point travel time)
  - Rate limiting awareness
  - Graceful error handling + fallback flag
"""

import json
import math
import structlog
from typing import Optional, Dict, Any, List, Tuple
from tenacity import retry, stop_after_attempt, wait_exponential, retry_if_exception_type

import googlemaps
from googlemaps.exceptions import ApiError, TransportError, Timeout

from config import settings

log = structlog.get_logger(__name__)


class GoogleMapsService:
    """
    Thin, typed wrapper around the googlemaps Python client.

    All methods return None on failure so callers can fall back
    to cached data gracefully.
    """

    def __init__(self):
        api_key = settings.GOOGLE_MAPS_API_KEY
        if not api_key or api_key == "YOUR_API_KEY_HERE":
            log.warning("Google Maps API key not set — using mock mode")
            self._client    = None
            self._mock_mode = True
        else:
            self._client    = googlemaps.Client(
                key=api_key,
                timeout=settings.GOOGLE_MAPS_TIMEOUT,
                retry_over_query_limit=True,
            )
            self._mock_mode = False

    # ─────────────────────────────────────────────────────────────────────
    # DIRECTIONS API
    # ─────────────────────────────────────────────────────────────────────

    @retry(
        stop=stop_after_attempt(3),
        wait=wait_exponential(multiplier=1, min=1, max=4),
        retry=retry_if_exception_type((TransportError, Timeout)),
        reraise=False,
    )
    def get_directions(
        self,
        origin_lat: float,
        origin_lng: float,
        dest_lat: float,
        dest_lng: float,
        waypoints: Optional[List[Tuple[float, float]]] = None,
        departure_time: str = "now",
        alternatives: bool = False,
    ) -> Optional[Dict[str, Any]]:
        """
        Call the Directions API.
        Returns a clean dict with: polyline, distance_km, duration_sec,
        duration_in_traffic_sec, steps.
        """
        if self._mock_mode:
            return self._mock_directions(origin_lat, origin_lng, dest_lat, dest_lng)

        try:
            origin = (origin_lat, origin_lng)
            dest   = (dest_lat, dest_lng)

            wp = None
            if waypoints:
                wp = [(lat, lng) for lat, lng in waypoints]

            result = self._client.directions(
                origin=origin,
                destination=dest,
                waypoints=wp,
                mode="driving",
                departure_time=departure_time,
                traffic_model="best_guess",
                alternatives=alternatives,
            )

            if not result:
                log.warning("Directions API returned empty result",
                            origin=origin, dest=dest)
                return None

            return self._parse_directions_result(result[0])

        except ApiError as e:
            log.error("Google Directions API error", error=str(e))
            return None
        except Exception as e:
            log.error("Unexpected error calling Directions API", error=str(e))
            return None

    def _parse_directions_result(self, route: Dict) -> Dict[str, Any]:
        """Extract the fields we care about from a raw Directions API result."""
        leg = route["legs"][0]    # single leg (driver → drop)

        distance_m        = leg["distance"]["value"]
        duration_sec      = leg["duration"]["value"]
        # duration_in_traffic may not exist if departure_time not set
        duration_traffic  = leg.get("duration_in_traffic", {}).get("value", duration_sec)

        # Encoded polyline (whole route)
        # Google API returns 'points' not 'encoded' in overview_polyline
        polyline = route["overview_polyline"].get("points") or route["overview_polyline"].get("encoded")

        # Parse steps
        steps = []
        for step in leg.get("steps", []):
            instruction = step.get("html_instructions", "")
            # Strip HTML tags
            import re
            instruction = re.sub(r"<[^>]+>", " ", instruction).strip()
            steps.append({
                "instruction":      instruction,
                "distance_meters":  step["distance"]["value"],
                "duration_seconds": step["duration"]["value"],
                "road_name":        step.get("road_name", ""),
            })

        # Congestion score: ratio of traffic duration to free-flow duration
        congestion_score = round(duration_traffic / max(duration_sec, 1), 3)
        congestion_score = min(congestion_score, 2.0)

        return {
            "polyline":               polyline,
            "distance_m":             distance_m,
            "distance_km":            round(distance_m / 1000, 3),
            "duration_sec":           duration_sec,
            "duration_in_traffic_sec": duration_traffic,
            "congestion_score":       congestion_score,
            "steps":                  steps,
            "total_turns":            len([s for s in steps if any(
                                          kw in s["instruction"].lower()
                                          for kw in ["turn", "left", "right", "merge", "exit"]
                                      )]),
        }

    # ─────────────────────────────────────────────────────────────────────
    # DISTANCE MATRIX API
    # ─────────────────────────────────────────────────────────────────────

    def get_distance_matrix(
        self,
        origins: List[Tuple[float, float]],
        destinations: List[Tuple[float, float]],
    ) -> Optional[Dict[str, Any]]:
        """
        Call the Distance Matrix API for multiple origin-destination pairs.
        Returns matrix of travel_time_sec and distance_km.
        """
        if self._mock_mode:
            return self._mock_distance_matrix(origins, destinations)

        try:
            result = self._client.distance_matrix(
                origins=origins,
                destinations=destinations,
                mode="driving",
                departure_time="now",
                traffic_model="best_guess",
            )
            return self._parse_distance_matrix(result)
        except Exception as e:
            log.error("Distance Matrix API error", error=str(e))
            return None

    def _parse_distance_matrix(self, result: Dict) -> Dict[str, Any]:
        rows = []
        for row in result.get("rows", []):
            elements = []
            for el in row.get("elements", []):
                if el["status"] == "OK":
                    elements.append({
                        "distance_km":     round(el["distance"]["value"] / 1000, 3),
                        "duration_sec":    el["duration"]["value"],
                        "duration_traffic_sec": el.get("duration_in_traffic", {}).get("value",
                                              el["duration"]["value"]),
                    })
                else:
                    elements.append(None)
            rows.append(elements)
        return {"rows": rows}

    # ─────────────────────────────────────────────────────────────────────
    # MOCK MODE (used when API key is not set — dev/testing)
    # ─────────────────────────────────────────────────────────────────────

    def _mock_directions(
        self, o_lat, o_lng, d_lat, d_lng
    ) -> Dict[str, Any]:
        """Return a realistic-looking mock response for local dev."""
        dist_km   = self._haversine_km(o_lat, o_lng, d_lat, d_lng) * 1.35
        duration  = int((dist_km / 30) * 3600)   # 30 km/h average in city
        traffic   = int(duration * 1.25)

        return {
            "polyline":                "mock_polyline_" + f"{o_lat:.3f}_{o_lng:.3f}",
            "distance_m":             int(dist_km * 1000),
            "distance_km":            round(dist_km, 3),
            "duration_sec":           duration,
            "duration_in_traffic_sec": traffic,
            "congestion_score":        round(traffic / max(duration, 1), 3),
            "steps": [
                {"instruction": "Head toward destination", "distance_meters": int(dist_km * 500),
                 "duration_seconds": duration // 2, "road_name": "Main Road"},
                {"instruction": "Turn right", "distance_meters": int(dist_km * 300),
                 "duration_seconds": duration // 4, "road_name": "Cross Road"},
                {"instruction": "Arrive at destination", "distance_meters": 0,
                 "duration_seconds": 0, "road_name": ""},
            ],
            "total_turns": 2,
        }

    def _mock_distance_matrix(self, origins, destinations):
        rows = []
        for o_lat, o_lng in origins:
            elements = []
            for d_lat, d_lng in destinations:
                dist_km  = self._haversine_km(o_lat, o_lng, d_lat, d_lng) * 1.35
                duration = int((dist_km / 30) * 3600)
                elements.append({
                    "distance_km":          round(dist_km, 3),
                    "duration_sec":         duration,
                    "duration_traffic_sec": int(duration * 1.2),
                })
            rows.append(elements)
        return {"rows": rows}

    @staticmethod
    def _haversine_km(lat1, lng1, lat2, lng2) -> float:
        R = 6371
        dlat = math.radians(lat2 - lat1)
        dlng = math.radians(lng2 - lng1)
        a = (math.sin(dlat/2)**2 +
             math.cos(math.radians(lat1)) * math.cos(math.radians(lat2)) *
             math.sin(dlng/2)**2)
        return R * 2 * math.asin(math.sqrt(a))

    def is_available(self) -> bool:
        return not self._mock_mode


# Global singleton
_gmaps: Optional[GoogleMapsService] = None

def get_gmaps() -> GoogleMapsService:
    global _gmaps
    if _gmaps is None:
        _gmaps = GoogleMapsService()
    return _gmaps
