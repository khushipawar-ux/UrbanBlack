"""
services/ — External service integrations for the Route Optimization Engine.

Exposes:
  - RouteOptimizer / get_optimizer() : core orchestrator (Cache → Google → ML)
  - GoogleMapsService / get_gmaps()  : Directions + Distance Matrix API wrapper
  - CacheService / get_cache()       : Redis cache with in-memory fallback
"""

from services.route_optimizer import RouteOptimizer, get_optimizer
from services.google_maps import GoogleMapsService, get_gmaps
from services.cache import CacheService, get_cache

__all__ = [
    "RouteOptimizer",
    "get_optimizer",
    "GoogleMapsService",
    "get_gmaps",
    "CacheService",
    "get_cache",
]
