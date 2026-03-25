import logging
import uuid
from datetime import datetime, timezone

from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, desc

from models import DriverLocation, LirTripGpsTrail
from schemas import RawGPSInput, SmoothedLocationOutput, LatestLocationResponse
from services.kalman_filter import smooth_gps
from services import kafka_producer

logger = logging.getLogger(__name__)

# In-memory GPS trail sequence counter per ride.
# Resets when a new ride_id is seen. Lightweight — sufficient for single-instance dev.
_ride_sequence: dict[str, int] = {}


def _next_sequence(ride_id: str) -> int:
    _ride_sequence[ride_id] = _ride_sequence.get(ride_id, 0) + 1
    return _ride_sequence[ride_id]


async def process_gps_fix(db: AsyncSession, raw: RawGPSInput) -> SmoothedLocationOutput:
    """
    Core pipeline for a single GPS fix from the driver app:
      1. Apply Kalman filter to smooth noisy raw GPS coords.
      2. Persist raw + smoothed data to `driver_locations`.
      3. If driver is on a trip, append smoothed point to `lir_trip_gps_trail`.
      4. Mark record as published and send to Kafka.
      5. Return the smoothed result to the caller.
    """
    # 1. Kalman filter smoothing
    smoothed_lat, smoothed_lng = smooth_gps(raw.driver_id, raw.lat, raw.lng)

    # 2. Persist to driver_locations
    now = raw.timestamp or datetime.now(timezone.utc)
    location_id = str(uuid.uuid4())

    loc = DriverLocation(
        id=location_id,
        driver_id=raw.driver_id,
        lat=raw.lat,
        lng=raw.lng,
        bearing=raw.bearing,
        speed_kmh=raw.speed_kmh,
        updated_at=now,
        smoothed_lat=smoothed_lat,
        smoothed_lng=smoothed_lng,
        accuracy_meters=raw.accuracy_meters,
        altitude=raw.altitude,
        gps_source=raw.gps_source,
        accel_x=raw.accel_x,
        accel_y=raw.accel_y,
        accel_z=raw.accel_z,
        is_on_trip=raw.is_on_trip,
        ride_id=raw.ride_id,
        published_to_kafka=True,
    )
    db.add(loc)

    # 3. If on a trip, record in GPS trail
    if raw.is_on_trip and raw.ride_id:
        seq = _next_sequence(raw.ride_id)

        # Compute segment_km as a simple Euclidean approximation (sufficient for telemetry)
        # In production this should use the Haversine formula or a PostGIS function.
        segment_km = None
        if raw.speed_kmh and raw.speed_kmh > 0:
            segment_km = round(raw.speed_kmh * (3.0 / 3600.0), 5)  # v * dt

        trail = LirTripGpsTrail(
            ride_id=raw.ride_id,
            driver_id=raw.driver_id,
            sequence_no=seq,
            lat=smoothed_lat,
            lng=smoothed_lng,
            speed_kmh=raw.speed_kmh,
            bearing=raw.bearing,
            recorded_at=now,
            segment_km=segment_km,
        )
        db.add(trail)

    await db.commit()

    # 4. Publish to Kafka (fire-and-forget; errors logged internally)
    await kafka_producer.publish_location(
        driver_id=raw.driver_id,
        smoothed_lat=smoothed_lat,
        smoothed_lng=smoothed_lng,
        bearing=raw.bearing,
        speed_kmh=raw.speed_kmh,
        ride_id=raw.ride_id,
        is_on_trip=raw.is_on_trip,
        location_id=location_id,
    )

    return SmoothedLocationOutput(
        driver_id=raw.driver_id,
        raw_lat=raw.lat,
        raw_lng=raw.lng,
        smoothed_lat=smoothed_lat,
        smoothed_lng=smoothed_lng,
        bearing=raw.bearing,
        speed_kmh=raw.speed_kmh,
        is_on_trip=raw.is_on_trip,
        ride_id=raw.ride_id,
        timestamp=now,
        location_id=location_id,
    )


async def get_latest_location(db: AsyncSession, driver_id: str) -> LatestLocationResponse | None:
    """Return the most recently persisted smoothed location for a driver."""
    result = await db.execute(
        select(DriverLocation)
        .where(DriverLocation.driver_id == driver_id)
        .order_by(desc(DriverLocation.updated_at))
        .limit(1)
    )
    loc = result.scalars().first()
    if not loc:
        return None

    return LatestLocationResponse(
        driver_id=driver_id,
        smoothed_lat=loc.smoothed_lat,
        smoothed_lng=loc.smoothed_lng,
        bearing=loc.bearing,
        speed_kmh=loc.speed_kmh,
        is_on_trip=loc.is_on_trip,
        updated_at=loc.updated_at,
    )
