import googlemaps
from datetime import datetime
import os
import logging

logger = logging.getLogger(__name__)

gmaps_client = None
try:
    gmaps_client = googlemaps.Client(key=os.getenv("GOOGLE_MAPS_API_KEY", "dummy_key"))
except Exception as e:
    logger.warning(f"Could not initialize Google Maps Client: {e}")

def get_directions(pickup_lat: float, pickup_lng: float, drop_lat: float, drop_lng: float, waypoints=None):
    if not gmaps_client:
        return None
        
    wp_str = [f"{wp.lat},{wp.lng}" for wp in waypoints] if waypoints else None
    try:
        # LIR-01 requirement: departure_time=now, traffic_model=best_guess
        res = gmaps_client.directions(
            f"{pickup_lat},{pickup_lng}",
            f"{drop_lat},{drop_lng}",
            waypoints=wp_str,
            departure_time=datetime.now(),
            traffic_model="best_guess",
            alternatives=False
        )
        return res
    except Exception as e:
        logger.error(f"Error calling google maps API: {e}")
        return None

def get_distance_matrix(origins, destinations):
    if not gmaps_client:
        return None
    try:
        return gmaps_client.distance_matrix(origins, destinations, departure_time=datetime.now(), traffic_model='best_guess')
    except Exception as e:
        logger.error(f"Error calling distance matrix: {e}")
        return None
