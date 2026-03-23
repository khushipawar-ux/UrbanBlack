from pydantic import BaseModel, Field
from typing import List, Optional
from datetime import datetime

class Point(BaseModel):
    lat: float
    lng: float

class PreTripPreviewRequest(BaseModel):
    user_id: str
    pickup: Point
    drop: Point
    vehicle_type: str = "SEDAN"
    waypoints: Optional[List[Point]] = None

class RoutePreviewResponse(BaseModel):
    ride_km: float
    fare_estimate: float
    driver_arrival_eta_sec: int
    ride_eta_sec: int
    ride_polyline: str
    approach_polyline: str
    routing_engine: str

class LiveRouteRequest(BaseModel):
    ride_id: str

class LiveRouteResponse(BaseModel):
    polyline: str
    distance_remaining_km: float
    eta_sec: int
