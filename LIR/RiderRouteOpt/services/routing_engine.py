import json
import logging
import uuid
from datetime import datetime
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select
from models import RideRoute, LirRouteSegment, Ride, FareConfig
from schemas import PreTripPreviewRequest, RoutePreviewResponse, LiveRouteRequest, LiveRouteResponse
from services.google_maps_service import get_directions
from services.feature_store import feature_store

logger = logging.getLogger(__name__)

async def compute_pre_trip_route(db: AsyncSession, req: PreTripPreviewRequest) -> RoutePreviewResponse:
    gmaps_res = get_directions(
        req.pickup.lat, req.pickup.lng, 
        req.drop.lat, req.drop.lng, 
        req.waypoints
    )
    
    ride_km = 0.0
    ride_eta_sec = 0
    polyline = "mock_overview_polyline"
    approach_polyline = "mock_approach_polyline"
    fare_estimate = 0.0
    
    if gmaps_res and len(gmaps_res) > 0:
        leg = gmaps_res[0]['legs'][0]
        ride_km = leg['distance']['value'] / 1000.0
        ride_eta_sec = leg['duration']['value']
        polyline = gmaps_res[0]['overview_polyline']['points']
    else:
        ride_km = 15.0
        ride_eta_sec = 2400

    # ML Congestion modifying ETA
    congestion_score = feature_store.get_segment_congestion_score(req.pickup.lat, req.pickup.lng, req.drop.lat, req.drop.lng)
    adjusted_eta_sec = int(ride_eta_sec * (1.0 + (congestion_score * 0.4)))
    
    try:
        result = await db.execute(select(FareConfig).where(FareConfig.vehicle_type == req.vehicle_type, FareConfig.is_active == True))
        fare_config = result.scalars().first()
        if fare_config:
            computed_fare = fare_config.base_fare + (ride_km * fare_config.per_km_rate) + ((adjusted_eta_sec/60.0) * fare_config.per_min_rate)
            fare_estimate = max(computed_fare, fare_config.minimum_fare)
        else:
            fare_estimate = 50.0 + (ride_km * 12.0)
    except Exception as e:
        logger.warning(f"Could not calculate fare from DB, using fallback: {e}")
        fare_estimate = 50.0 + (ride_km * 12.0)
        
    driver_arrival_eta_sec = 300 # Mock API response
    
    # Ideally, write to db here. ride_id is required, so we skip insert until ride is formally created.
    return RoutePreviewResponse(
        ride_km=round(ride_km, 2),
        fare_estimate=round(fare_estimate, 2),
        driver_arrival_eta_sec=driver_arrival_eta_sec,
        ride_eta_sec=adjusted_eta_sec,
        ride_polyline=polyline,
        approach_polyline=approach_polyline,
        routing_engine="LIR_Blended_v1"
    )

async def compute_live_route(db: AsyncSession, req: LiveRouteRequest) -> LiveRouteResponse:
    # Look up driver location and ride data, check for > 300m deviation and re-route if needed
    # For now, return mock live response
    return LiveRouteResponse(
        polyline="live_mock_polyline",
        distance_remaining_km=5.2,
        eta_sec=600
    )
