"""
models/ — Pydantic schemas for all API request and response types.

Request models:
  - RouteRequest          : GET /driver/route
  - ETARequest            : GET /driver/eta
  - RerouteRequest        : POST /driver/reroute
  - LocationUpdateRequest : POST /driver/update-location

Response models:
  - RouteResponse         : polyline + ML ETA + steps
  - ETAResponse           : ML ETA only
  - RerouteResponse       : new route if reroute triggered
  - LocationUpdateResponse
  - HealthResponse        : /health status

Shared types:
  - Coordinates           : {lat, lng}
  - NavigationStep        : one turn-by-turn instruction

Enums:
  - RideStatus / WeatherCondition / RerouteTrigger
"""

from models.schemas import (
    # Requests
    RouteRequest,
    ETARequest,
    RerouteRequest,
    LocationUpdateRequest,
    # Responses
    RouteResponse,
    ETAResponse,
    RerouteResponse,
    LocationUpdateResponse,
    HealthResponse,
    # Shared
    Coordinates,
    NavigationStep,
    # Enums
    RideStatus,
    WeatherCondition,
    RerouteTrigger,
)

__all__ = [
    "RouteRequest",
    "ETARequest",
    "RerouteRequest",
    "LocationUpdateRequest",
    "RouteResponse",
    "ETAResponse",
    "RerouteResponse",
    "LocationUpdateResponse",
    "HealthResponse",
    "Coordinates",
    "NavigationStep",
    "RideStatus",
    "WeatherCondition",
    "RerouteTrigger",
]
