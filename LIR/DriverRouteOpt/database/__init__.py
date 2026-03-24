"""
database/ — Async PostgreSQL layer for the Route Optimization Engine.

Exposes:
  - create_pool()        : initialise the asyncpg connection pool (call on startup)
  - close_pool()         : gracefully close the pool (call on shutdown)
  - check_connection()   : ping the DB — used by /health endpoint
  - get_pool()           : return the live pool for raw queries

  Query helpers (all async):
  - log_eta_prediction()      : write every ML inference to eta_predictions table
  - log_reroute_event()       : write reroute decisions to reroute_events table
  - update_driver_location()  : update drivers.current_lat / current_lng
  - get_traffic_pattern()     : read historical congestion for a road + time slot
  - get_nearby_drivers()      : find online drivers within a radius
  - save_route_cache_to_db()  : mirror Redis cache into route_cache table (audit/fallback)
  - get_active_model_version(): read the current production model tag from model_versions
"""

from database.connection import create_pool, close_pool, check_connection, get_pool
from database.queries import (
    log_eta_prediction,
    log_reroute_event,
    update_driver_location,
    get_traffic_pattern,
    get_nearby_drivers,
    get_historical_route_avg,
    save_route_cache_to_db,
    get_active_model_version,
)

__all__ = [
    # Connection
    "create_pool",
    "close_pool",
    "check_connection",
    "get_pool",
    # Queries
    "log_eta_prediction",
    "log_reroute_event",
    "update_driver_location",
    "get_traffic_pattern",
    "get_nearby_drivers",
    "get_historical_route_avg",
    "save_route_cache_to_db",
    "get_active_model_version",
]
