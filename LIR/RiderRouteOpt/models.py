from sqlalchemy import Column, String, Float, Integer, Boolean, DateTime, ForeignKey, Text, Enum, func
from sqlalchemy.dialects.postgresql import JSON
from geoalchemy2 import Geometry
from database import Base

class Ride(Base):
    __tablename__ = 'rides'
    id = Column(String, primary_key=True, server_default=func.gen_random_uuid())
    user_id = Column(String, nullable=False)
    driver_id = Column(String)  # fk to driver
    pickup_lat = Column(Float, nullable=False)
    pickup_lng = Column(Float, nullable=False)
    drop_lat = Column(Float, nullable=False)
    drop_lng = Column(Float, nullable=False)
    status = Column(String, nullable=False, default='REQUESTED')
    ride_km = Column(Float)
    duration_min = Column(Integer)
    fare = Column(Float)
    requested_at = Column(DateTime(timezone=True))
    started_at = Column(DateTime(timezone=True))
    completed_at = Column(DateTime(timezone=True))
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    # LIR 
    optimized_pickup_lat = Column(Float)
    optimized_pickup_lng = Column(Float)
    
class DriverLocation(Base):
    __tablename__ = 'driver_locations'
    id = Column(String, primary_key=True, server_default=func.gen_random_uuid())
    driver_id = Column(String, nullable=False)
    lat = Column(Float, nullable=False)
    lng = Column(Float, nullable=False)
    updated_at = Column(DateTime(timezone=True), server_default=func.now())
    # LIR
    smoothed_lat = Column(Float)
    smoothed_lng = Column(Float)
    is_on_trip = Column(Boolean, default=False)
    ride_id = Column(String, ForeignKey('rides.id'))
    
class LirTrafficZone(Base):
    __tablename__ = 'lir_traffic_zones'
    id = Column(String, primary_key=True, server_default=func.gen_random_uuid())
    zone_name = Column(String, nullable=False)
    zone_type = Column(String, nullable=False) # TOLL, POLICE_ZONE, SCHOOL_ZONE, etc.
    polygon_json = Column(Text, nullable=False) # Can be used for bounding box constraints
    speed_limit_kmh = Column(Integer)
    is_active = Column(Boolean, default=True)

class RideRoute(Base):
    __tablename__ = 'ride_routes'
    id = Column(String, primary_key=True, server_default=func.gen_random_uuid())
    ride_id = Column(String, ForeignKey('rides.id'), nullable=False)
    approach_polyline = Column(Text)
    ride_polyline = Column(Text)
    approach_km = Column(Float)
    ride_km = Column(Float)
    routing_engine = Column(String)
    traffic_snapshot_at = Column(DateTime(timezone=True))
    waypoints_json = Column(Text)
    computed_at = Column(DateTime(timezone=True), server_default=func.now())
    
class LirRouteSegment(Base):
    __tablename__ = 'lir_route_segments'
    id = Column(String, primary_key=True, server_default=func.gen_random_uuid())
    ride_route_id = Column(String, ForeignKey('ride_routes.id'), nullable=False)
    segment_index = Column(Integer, nullable=False)
    start_lat = Column(Float, nullable=False)
    start_lng = Column(Float, nullable=False)
    end_lat = Column(Float, nullable=False)
    end_lng = Column(Float, nullable=False)
    predicted_sec = Column(Float, nullable=False)
    actual_sec = Column(Float) # For nightly retraining pipeline
    congestion_score = Column(Float) # Output of ML Feature store
    road_class = Column(String)
    has_toll_gate = Column(Boolean, default=False)
    created_at = Column(DateTime(timezone=True), server_default=func.now())

class FareConfig(Base):
    __tablename__ = 'fare_config'
    id = Column(String, primary_key=True, server_default=func.gen_random_uuid())
    city = Column(String, nullable=False)
    vehicle_type = Column(String, nullable=False)
    base_fare = Column(Float, nullable=False)
    per_km_rate = Column(Float, nullable=False)
    per_min_rate = Column(Float, nullable=False)
    minimum_fare = Column(Float, nullable=False)
    is_active = Column(Boolean, default=True)
