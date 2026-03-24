"""
models/schemas.py — Pydantic request & response models
All API inputs and outputs are typed and validated here.
"""

from pydantic import BaseModel, Field, field_validator
from typing import Optional, List
from enum import Enum


# ─────────────────────────────────────────────────────────────────────────────
# ENUMS
# ─────────────────────────────────────────────────────────────────────────────

class RideStatus(str, Enum):
    REQUESTED  = "requested"
    ASSIGNED   = "assigned"
    ONGOING    = "ongoing"
    COMPLETED  = "completed"
    CANCELLED  = "cancelled"

class WeatherCondition(str, Enum):
    CLEAR   = "clear"
    CLOUDY  = "cloudy"
    RAINY   = "rainy"
    STORMY  = "stormy"

class RerouteTrigger(str, Enum):
    TRAFFIC_CHANGE  = "traffic_change"
    GPS_DEVIATION   = "gps_deviation"
    ACCIDENT        = "accident"
    ROAD_CLOSURE    = "road_closure"
    DRIVER_REQUEST  = "driver_request"
    APP_POLL        = "app_poll"


# ─────────────────────────────────────────────────────────────────────────────
# SHARED COORDINATE MODEL
# ─────────────────────────────────────────────────────────────────────────────

class Coordinates(BaseModel):
    lat: float = Field(..., ge=-90,  le=90,  description="Latitude")
    lng: float = Field(..., ge=-180, le=180, description="Longitude")


# ─────────────────────────────────────────────────────────────────────────────
# REQUEST MODELS
# ─────────────────────────────────────────────────────────────────────────────

class RouteRequest(BaseModel):
    """GET /driver/route — Full route from driver → pickup → drop."""
    driver_lat:  float = Field(..., ge=-90,  le=90,  example=18.5362)
    driver_lng:  float = Field(..., ge=-180, le=180, example=73.8935)
    pickup_lat:  float = Field(..., ge=-90,  le=90,  example=18.5309)
    pickup_lng:  float = Field(..., ge=-180, le=180, example=73.8474)
    drop_lat:    float = Field(..., ge=-90,  le=90,  example=18.5912)
    drop_lng:    float = Field(..., ge=-180, le=180, example=73.7389)
    driver_id:   Optional[str] = Field(None, description="Driver UUID for logging")
    ride_id:     Optional[str] = Field(None, description="Ride UUID for logging")

    @field_validator("driver_lat", "pickup_lat", "drop_lat")
    @classmethod
    def lat_in_pune_range(cls, v):
        # Pune bounding box: lat 18.40–18.70
        if not (17.5 <= v <= 20.0):
            raise ValueError("Latitude out of Maharashtra range")
        return round(v, 7)

    @field_validator("driver_lng", "pickup_lng", "drop_lng")
    @classmethod
    def lng_in_pune_range(cls, v):
        # Pune bounding box: lng 73.65–74.05
        if not (72.0 <= v <= 75.0):
            raise ValueError("Longitude out of Maharashtra range")
        return round(v, 7)


class ETARequest(BaseModel):
    """GET /driver/eta — Only ETA, no full route details."""
    origin_lat:  float = Field(..., ge=-90,  le=90)
    origin_lng:  float = Field(..., ge=-180, le=180)
    dest_lat:    float = Field(..., ge=-90,  le=90)
    dest_lng:    float = Field(..., ge=-180, le=180)
    driver_id:   Optional[str] = None
    ride_id:     Optional[str] = None


class RerouteRequest(BaseModel):
    """POST /driver/reroute — Mid-trip reroute from current driver position."""
    ride_id:          str   = Field(..., description="Active ride UUID")
    driver_id:        str   = Field(..., description="Driver UUID")
    current_lat:      float = Field(..., ge=-90,  le=90)
    current_lng:      float = Field(..., ge=-180, le=180)
    drop_lat:         float = Field(..., ge=-90,  le=90)
    drop_lng:         float = Field(..., ge=-180, le=180)
    current_eta_min:  float = Field(..., gt=0, description="Current ETA before reroute")
    trigger:          RerouteTrigger = RerouteTrigger.APP_POLL


class LocationUpdateRequest(BaseModel):
    """POST /driver/update-location — Receive live GPS from driver app."""
    driver_id:       str
    ride_id:         Optional[str] = None
    latitude:        float = Field(..., ge=-90,  le=90)
    longitude:       float = Field(..., ge=-180, le=180)
    speed_kmph:      float = Field(0.0, ge=0, le=200)
    heading_degrees: Optional[int] = Field(None, ge=0, le=360)
    accuracy_meters: Optional[float] = None


# ─────────────────────────────────────────────────────────────────────────────
# RESPONSE MODELS
# ─────────────────────────────────────────────────────────────────────────────

class NavigationStep(BaseModel):
    instruction:      str
    distance_meters:  int
    duration_seconds: int
    road_name:        Optional[str] = None


class RouteResponse(BaseModel):
    """Response for /driver/route"""
    ride_id:          Optional[str]
    driver_id:        Optional[str]

    # Route data
    polyline:         str             = Field(..., description="Google encoded polyline")
    distance_km:      float
    steps:            List[NavigationStep]

    # ETA
    google_eta_min:   float           = Field(..., description="Raw Google ETA in minutes")
    ml_eta_min:       float           = Field(..., description="ML-corrected ETA in minutes")
    eta_correction_min: float         = Field(..., description="Difference: ml_eta - google_eta")

    # Metadata
    served_from_cache: bool           = False
    response_time_ms:  int            = 0
    model_version:     str            = "lgbm_v1.0"
    traffic_condition: str            = "normal"   # free / normal / heavy / severe
    is_peak_hour:      bool           = False
    weather_condition: str            = "clear"


class ETAResponse(BaseModel):
    """Response for /driver/eta"""
    origin_lat:       float
    origin_lng:       float
    dest_lat:         float
    dest_lng:         float
    distance_km:      float
    google_eta_min:   float
    ml_eta_min:       float
    eta_correction_min: float
    is_peak_hour:     bool
    traffic_condition: str
    model_version:    str
    response_time_ms: int


class RerouteResponse(BaseModel):
    """Response for /driver/reroute"""
    ride_id:           str
    rerouted:          bool          = Field(..., description="True if a new route was computed")
    reason:            str           = ""

    # New route (only populated if rerouted=True)
    new_polyline:      Optional[str] = None
    new_distance_km:   Optional[float] = None
    new_eta_min:       Optional[float] = None
    old_eta_min:       float
    time_saved_min:    Optional[float] = None   # negative = reroute added time

    steps:             Optional[List[NavigationStep]] = None
    response_time_ms:  int = 0


class LocationUpdateResponse(BaseModel):
    driver_id:       str
    received:        bool = True
    needs_reroute:   bool = False
    message:         str  = "Location updated"


class HealthResponse(BaseModel):
    status:          str
    version:         str
    model_loaded:    bool
    db_connected:    bool
    redis_connected: bool
    google_maps_ok:  bool
