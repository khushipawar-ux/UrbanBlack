from sqlalchemy import Column, String, Float, Integer, Boolean, DateTime, ForeignKey, func
from sqlalchemy.dialects.postgresql import ENUM
from database import Base


class DriverLocation(Base):
    """
    Maps to the existing `driver_locations` table in the LIR schema.
    Stores both raw GPS readings and Kalman-smoothed coordinates.
    """
    __tablename__ = 'driver_locations'

    id = Column(String, primary_key=True, server_default=func.gen_random_uuid())
    driver_id = Column(String, nullable=False)
    lat = Column(Float, nullable=False)          # raw GPS lat
    lng = Column(Float, nullable=False)          # raw GPS lng
    bearing = Column(Float)
    speed_kmh = Column(Float)
    updated_at = Column(DateTime(timezone=True), server_default=func.now())
    # LIR additions — Kalman outputs
    smoothed_lat = Column(Float)
    smoothed_lng = Column(Float)
    accuracy_meters = Column(Float)
    altitude = Column(Float)
    gps_source = Column(ENUM('GPS', 'NETWORK', 'FUSED', 'IMU_DEAD_RECKONING', name='gps_source_enum', create_type=False))
    accel_x = Column(Float)
    accel_y = Column(Float)
    accel_z = Column(Float)
    is_on_trip = Column(Boolean, default=False)
    ride_id = Column(String)
    published_to_kafka = Column(Boolean, default=False)


class LirTripGpsTrail(Base):
    """
    Maps to the existing `lir_trip_gps_trail` table.
    Records the sequence of smoothed GPS fixes during an active ride.
    """
    __tablename__ = 'lir_trip_gps_trail'

    id = Column(String, primary_key=True, server_default=func.gen_random_uuid())
    ride_id = Column(String, nullable=False)
    driver_id = Column(String, nullable=False)
    sequence_no = Column(Integer, nullable=False)
    lat = Column(Float, nullable=False)          # smoothed lat
    lng = Column(Float, nullable=False)          # smoothed lng
    speed_kmh = Column(Float)
    bearing = Column(Float)
    recorded_at = Column(DateTime(timezone=True), nullable=False, server_default=func.now())
    segment_km = Column(Float)
