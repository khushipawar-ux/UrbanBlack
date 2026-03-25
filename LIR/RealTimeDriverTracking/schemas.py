from pydantic import BaseModel, Field
from typing import Optional
from datetime import datetime


class RawGPSInput(BaseModel):
    """Incoming GPS fix from the driver app."""
    driver_id: str
    lat: float = Field(..., ge=-90, le=90)
    lng: float = Field(..., ge=-180, le=180)
    accuracy_meters: Optional[float] = None
    altitude: Optional[float] = None
    bearing: Optional[float] = None
    speed_kmh: Optional[float] = None
    gps_source: Optional[str] = "FUSED"         # GPS | NETWORK | FUSED | IMU_DEAD_RECKONING
    accel_x: Optional[float] = None
    accel_y: Optional[float] = None
    accel_z: Optional[float] = None
    is_on_trip: bool = False
    ride_id: Optional[str] = None
    timestamp: Optional[datetime] = None


class SmoothedLocationOutput(BaseModel):
    """Kalman-filtered location result — returned to caller and published to Kafka."""
    driver_id: str
    raw_lat: float
    raw_lng: float
    smoothed_lat: float
    smoothed_lng: float
    bearing: Optional[float]
    speed_kmh: Optional[float]
    is_on_trip: bool
    ride_id: Optional[str]
    timestamp: datetime
    location_id: str                             # persisted DB id


class TrackingSessionRequest(BaseModel):
    """Start/stop a driver tracking session (shift boundary)."""
    driver_id: str
    ride_id: Optional[str] = None


class TrackingSessionResponse(BaseModel):
    driver_id: str
    status: str                                  # 'started' | 'stopped'
    message: str


class LatestLocationResponse(BaseModel):
    """Most recent smoothed location for a driver."""
    driver_id: str
    smoothed_lat: Optional[float]
    smoothed_lng: Optional[float]
    bearing: Optional[float]
    speed_kmh: Optional[float]
    is_on_trip: bool
    updated_at: Optional[datetime]
