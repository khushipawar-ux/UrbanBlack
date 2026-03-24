"""
services/route_optimizer.py — Core Route Optimization Engine

This is the heart of the system. It:
  1. Checks Redis cache (fast path)
  2. Calls Google Directions API (base route)
  3. Runs ML ETA prediction (LightGBM correction)
  4. Decides if an alternative route is worth suggesting
  5. Logs every prediction to the database
  6. Returns a clean RouteResponse

This file is called by the FastAPI endpoints in main.py.
"""

import time
import json
import structlog
from datetime import datetime
from typing import Optional, Dict, Any, List

from services.google_maps import get_gmaps, GoogleMapsService
from services.cache import get_cache, CacheService
from ml.predictor import get_predictor, ETAPredictor
from database.queries import (
    log_eta_prediction,
    log_reroute_event,
    save_route_cache_to_db,
)
from models.schemas import (
    RouteResponse,
    ETAResponse,
    RerouteResponse,
    NavigationStep,
)
from config import settings

log = structlog.get_logger(__name__)


class RouteOptimizer:
    """
    Orchestrates: Cache → Google Maps → ML Model → Response

    Architecture:
        Driver App → RouteOptimizer → [Redis Cache | Google Directions API]
                                    → LightGBM ETA Model
                                    → RouteResponse (JSON)
    """

    def __init__(
        self,
        gmaps:     Optional[GoogleMapsService] = None,
        cache:     Optional[CacheService]      = None,
        predictor: Optional[ETAPredictor]      = None,
    ):
        self.gmaps     = gmaps     or get_gmaps()
        self.cache     = cache     or get_cache()
        self.predictor = predictor or get_predictor()

    # ─────────────────────────────────────────────────────────────────────
    # 1. GET FULL ROUTE (driver → pickup → drop)
    # ─────────────────────────────────────────────────────────────────────

    async def get_route(
        self,
        driver_lat:  float,
        driver_lng:  float,
        pickup_lat:  float,
        pickup_lng:  float,
        drop_lat:    float,
        drop_lng:    float,
        driver_id:   Optional[str] = None,
        ride_id:     Optional[str] = None,
    ) -> RouteResponse:
        """
        Full route: driver → pickup → drop.
        Returns polyline, ML-corrected ETA, turn-by-turn steps.
        """
        t_start = time.monotonic()

        # ── Step 1: Check cache (driver → drop, ignoring exact driver pos) ──
        cached = await self.cache.get_route(pickup_lat, pickup_lng, drop_lat, drop_lng)
        if cached:
            # Still run ML on cached route for fresh ETA (traffic may have changed)
            ml_result = self._run_ml(
                distance_km=cached["distance_km"],
                google_eta_sec=cached["duration_in_traffic_sec"],
            )
            response_ms = int((time.monotonic() - t_start) * 1000)

            # Async log prediction (fire-and-forget)
            await self._log_prediction_async(
                ride_id, driver_id, ml_result,
                cached["distance_km"], cached["duration_in_traffic_sec"],
                "initial"
            )

            return self._build_route_response(
                ride_id=ride_id,
                driver_id=driver_id,
                directions=cached,
                ml_result=ml_result,
                served_from_cache=True,
                response_ms=response_ms,
            )

        # ── Step 2: Call Google Directions API ─────────────────────────────
        # Pass pickup as waypoint so we get: driver→pickup→drop in one call
        directions = self.gmaps.get_directions(
            origin_lat=driver_lat,
            origin_lng=driver_lng,
            dest_lat=drop_lat,
            dest_lng=drop_lng,
            waypoints=[(pickup_lat, pickup_lng)],
        )

        if directions is None:
            # Google API failed — try to serve last known cache for this route
            last_cache = await self.cache.get_route(pickup_lat, pickup_lng, drop_lat, drop_lng)
            if last_cache:
                log.warning("Google API failed, serving stale cache", ride_id=ride_id)
                ml_result = self._run_ml(
                    distance_km=last_cache["distance_km"],
                    google_eta_sec=last_cache["duration_in_traffic_sec"],
                )
                return self._build_route_response(
                    ride_id, driver_id, last_cache, ml_result,
                    served_from_cache=True,
                    response_ms=int((time.monotonic() - t_start) * 1000),
                )
            raise RuntimeError("Google Maps API unavailable and no cache fallback")

        # ── Step 3: ML ETA prediction ────────────────────────────────────
        ml_result = self._run_ml(
            distance_km=directions["distance_km"],
            google_eta_sec=directions["duration_in_traffic_sec"],
        )

        # ── Step 4: Cache the Google result ──────────────────────────────
        await self.cache.set_route(pickup_lat, pickup_lng, drop_lat, drop_lng, directions)
        # Mirror to DB async
        await self._save_db_cache_async(
            pickup_lat, pickup_lng, drop_lat, drop_lng, directions
        )

        # ── Step 5: Log prediction ────────────────────────────────────────
        await self._log_prediction_async(
            ride_id, driver_id, ml_result,
            directions["distance_km"], directions["duration_in_traffic_sec"],
            "initial"
        )

        response_ms = int((time.monotonic() - t_start) * 1000)
        log.info("Route computed",
                 ride_id=ride_id,
                 distance_km=directions["distance_km"],
                 google_eta_min=ml_result["google_eta_min"],
                 ml_eta_min=ml_result["ml_eta_min"],
                 response_ms=response_ms)

        return self._build_route_response(
            ride_id, driver_id, directions, ml_result,
            served_from_cache=False,
            response_ms=response_ms,
        )

    # ─────────────────────────────────────────────────────────────────────
    # 2. GET ETA ONLY
    # ─────────────────────────────────────────────────────────────────────

    async def get_eta(
        self,
        origin_lat:  float,
        origin_lng:  float,
        dest_lat:    float,
        dest_lng:    float,
        driver_id:   Optional[str] = None,
        ride_id:     Optional[str] = None,
    ) -> ETAResponse:
        """Lightweight ETA call — no polyline, just ML-corrected time."""
        t_start = time.monotonic()
        from datetime import timezone, timedelta
        IST_OFFSET = timezone(timedelta(hours=5, minutes=30))
        now     = datetime.now(IST_OFFSET)

        # Check cache for ETA
        eta_key = self.cache.make_eta_key(origin_lat, origin_lng, dest_lat, dest_lng, now.hour)
        cached  = await self.cache.get(eta_key)
        if cached:
            log.info("ETA cache HIT")
            return ETAResponse(**cached, response_time_ms=int((time.monotonic()-t_start)*1000))

        directions = self.gmaps.get_directions(
            origin_lat=origin_lat,
            origin_lng=origin_lng,
            dest_lat=dest_lat,
            dest_lng=dest_lng,
        )

        if directions is None:
            # Fallback: straight-line heuristic
            import math
            dlat = math.radians(dest_lat - origin_lat)
            dlng = math.radians(dest_lng - origin_lng)
            a = math.sin(dlat/2)**2 + math.cos(math.radians(origin_lat))*math.cos(math.radians(dest_lat))*math.sin(dlng/2)**2
            dist_km = 6371 * 2 * math.asin(math.sqrt(a)) * 1.35
            google_eta_sec = int((dist_km / 30) * 3600)
            directions = {"distance_km": dist_km, "duration_in_traffic_sec": google_eta_sec,
                         "congestion_score": 1.5}

        ml_result = self._run_ml(
            distance_km=directions["distance_km"],
            google_eta_sec=directions["duration_in_traffic_sec"],
        )

        response_payload = {
            "origin_lat":        origin_lat,
            "origin_lng":        origin_lng,
            "dest_lat":          dest_lat,
            "dest_lng":          dest_lng,
            "distance_km":       round(directions["distance_km"], 3),
            "google_eta_min":    ml_result["google_eta_min"],
            "ml_eta_min":        ml_result["ml_eta_min"],
            "eta_correction_min": ml_result["eta_correction_min"],
            "is_peak_hour":      ml_result["is_peak_hour"],
            "traffic_condition": ml_result["traffic_condition"],
            "model_version":     ml_result["model_version"],
        }

        await self.cache.set(eta_key, response_payload, ttl=180)   # 3 min TTL for ETA
        response_ms = int((time.monotonic() - t_start) * 1000)
        return ETAResponse(**response_payload, response_time_ms=response_ms)

    # ─────────────────────────────────────────────────────────────────────
    # 3. REROUTE (mid-trip)
    # ─────────────────────────────────────────────────────────────────────

    async def reroute(
        self,
        ride_id:         str,
        driver_id:       str,
        current_lat:     float,
        current_lng:     float,
        drop_lat:        float,
        drop_lng:        float,
        current_eta_min: float,
        trigger:         str = "app_poll",
    ) -> RerouteResponse:
        """
        Recompute the route from driver's current position to drop.
        Only returns a new route if it's meaningfully different from current.
        """
        t_start = time.monotonic()

        # Invalidate cache for current position (it's a new origin)
        new_directions = self.gmaps.get_directions(
            origin_lat=current_lat,
            origin_lng=current_lng,
            dest_lat=drop_lat,
            dest_lng=drop_lng,
        )

        if new_directions is None:
            return RerouteResponse(
                ride_id=ride_id,
                rerouted=False,
                reason="Google Maps API unavailable",
                old_eta_min=current_eta_min,
                response_time_ms=int((time.monotonic()-t_start)*1000),
            )

        ml_result = self._run_ml(
            distance_km=new_directions["distance_km"],
            google_eta_sec=new_directions["duration_in_traffic_sec"],
        )

        new_eta_min  = ml_result["ml_eta_min"]
        time_saved   = round(current_eta_min - new_eta_min, 2)

        # Decide if reroute is worth it:
        # Reroute if new route saves ≥5% time OR if old ETA is significantly wrong
        improvement_pct = (current_eta_min - new_eta_min) / max(current_eta_min, 1)
        should_reroute  = improvement_pct >= settings.ML_IMPROVEMENT_THRESHOLD

        # Always reroute if trigger is a hard event (accident/road_closure)
        if trigger in ("accident", "road_closure"):
            should_reroute = True

        # Log reroute event to DB
        await log_reroute_event(
            ride_id=ride_id,
            driver_id=driver_id,
            trigger_reason=trigger,
            driver_lat=current_lat,
            driver_lng=current_lng,
            old_eta_min=current_eta_min,
            new_eta_min=new_eta_min,
            old_distance_km=new_directions["distance_km"],   # we don't have old dist here
            new_distance_km=new_directions["distance_km"],
            response_ms=int((time.monotonic()-t_start)*1000),
        )

        if not should_reroute:
            return RerouteResponse(
                ride_id=ride_id,
                rerouted=False,
                reason=f"Current route optimal (improvement {improvement_pct*100:.1f}% < 5% threshold)",
                old_eta_min=current_eta_min,
                new_eta_min=new_eta_min,
                response_time_ms=int((time.monotonic()-t_start)*1000),
            )

        # Build navigation steps
        steps = [NavigationStep(**s) for s in new_directions.get("steps", [])]

        # Log new prediction
        await self._log_prediction_async(
            ride_id, driver_id, ml_result,
            new_directions["distance_km"], new_directions["duration_in_traffic_sec"],
            "reroute"
        )

        response_ms = int((time.monotonic() - t_start) * 1000)
        log.info("Reroute computed",
                 ride_id=ride_id,
                 old_eta=current_eta_min,
                 new_eta=new_eta_min,
                 time_saved=time_saved,
                 trigger=trigger,
                 response_ms=response_ms)

        return RerouteResponse(
            ride_id=ride_id,
            rerouted=True,
            reason=f"Saved {time_saved:.1f} min via reroute ({trigger})",
            new_polyline=new_directions["polyline"],
            new_distance_km=new_directions["distance_km"],
            new_eta_min=new_eta_min,
            old_eta_min=current_eta_min,
            time_saved_min=time_saved,
            steps=steps,
            response_time_ms=response_ms,
        )

    # ─────────────────────────────────────────────────────────────────────
    # INTERNAL HELPERS
    # ─────────────────────────────────────────────────────────────────────

    def _run_ml(
        self,
        distance_km: float,
        google_eta_sec: int,
    ) -> Dict[str, Any]:
        """Run ML prediction with current time context."""
        from datetime import timezone, timedelta
        IST_OFFSET = timezone(timedelta(hours=5, minutes=30))
        now = datetime.now(IST_OFFSET)
        return self.predictor.predict(
            distance_km=distance_km,
            google_eta_sec=google_eta_sec,
            hour_of_day=now.hour,
            day_of_week=now.weekday(),
        )

    def _build_route_response(
        self,
        ride_id: Optional[str],
        driver_id: Optional[str],
        directions: Dict[str, Any],
        ml_result: Dict[str, Any],
        served_from_cache: bool,
        response_ms: int,
    ) -> RouteResponse:
        steps = [NavigationStep(**s) for s in directions.get("steps", [])]
        return RouteResponse(
            ride_id=ride_id,
            driver_id=driver_id,
            polyline=directions["polyline"],
            distance_km=directions["distance_km"],
            steps=steps,
            google_eta_min=ml_result["google_eta_min"],
            ml_eta_min=ml_result["ml_eta_min"],
            eta_correction_min=ml_result["eta_correction_min"],
            served_from_cache=served_from_cache,
            response_time_ms=response_ms,
            model_version=ml_result["model_version"],
            traffic_condition=ml_result["traffic_condition"],
            is_peak_hour=ml_result["is_peak_hour"],
            weather_condition=ml_result["features"].get("weather_condition", "clear"),
        )

    async def _log_prediction_async(
        self,
        ride_id, driver_id, ml_result, distance_km, google_eta_sec, prediction_type
    ):
        """Log to DB — non-blocking, errors are swallowed."""
        try:
            from datetime import timezone, timedelta
            IST_OFFSET = timezone(timedelta(hours=5, minutes=30))
            now = datetime.now(IST_OFFSET)
            await log_eta_prediction(
                ride_id=ride_id,
                driver_id=driver_id,
                model_version=ml_result["model_version"],
                prediction_type=prediction_type,
                distance_km=distance_km,
                google_eta_sec=google_eta_sec,
                hour_of_day=now.hour,
                day_of_week=now.weekday(),
                is_weekend=now.weekday() in (5, 6),
                is_peak_hour=ml_result["is_peak_hour"],
                traffic_score=ml_result["features"].get("traffic_congestion_score", 2.0),
                weather_condition=ml_result["features"].get("weather_condition", "clear"),
                rainfall_mm=ml_result["features"].get("rainfall_mm", 0.0),
                google_eta_min=ml_result["google_eta_min"],
                ml_eta_min=ml_result["ml_eta_min"],
                eta_correction_min=ml_result["eta_correction_min"],
                confidence_score=ml_result.get("confidence_score"),
            )
        except Exception as e:
            log.warning("Prediction logging failed (non-critical)", error=str(e))

    async def _save_db_cache_async(
        self, pickup_lat, pickup_lng, drop_lat, drop_lng, directions
    ):
        try:
            cache_key = self.cache.make_route_key(pickup_lat, pickup_lng, drop_lat, drop_lng)
            await save_route_cache_to_db(
                cache_key=cache_key,
                origin_lat=pickup_lat, origin_lng=pickup_lng,
                dest_lat=drop_lat,     dest_lng=drop_lng,
                polyline=directions["polyline"],
                distance_km=directions["distance_km"],
                google_duration_sec=directions["duration_sec"],
                google_eta_traffic_sec=directions["duration_in_traffic_sec"],
                steps_json=json.dumps(directions.get("steps", [])),
                ttl_seconds=settings.CACHE_TTL_SECONDS,
            )
        except Exception as e:
            log.warning("DB cache mirror failed (non-critical)", error=str(e))


# Global singleton
_optimizer: Optional[RouteOptimizer] = None

def get_optimizer() -> RouteOptimizer:
    global _optimizer
    if _optimizer is None:
        _optimizer = RouteOptimizer()
    return _optimizer
