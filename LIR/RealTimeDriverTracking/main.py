import os
import logging
from contextlib import asynccontextmanager
from dotenv import load_dotenv

load_dotenv()

from fastapi import FastAPI, Depends, HTTPException
from sqlalchemy.ext.asyncio import AsyncSession

from database import engine, Base, get_db
from schemas import (
    RawGPSInput,
    SmoothedLocationOutput,
    TrackingSessionRequest,
    TrackingSessionResponse,
    LatestLocationResponse,
)
from services.location_service import process_gps_fix, get_latest_location
from services import kafka_producer
from services.kalman_filter import reset_filter

logging.basicConfig(level=os.getenv("LOG_LEVEL", "INFO"))
logger = logging.getLogger(__name__)


async def init_db():
    """Create tables on fresh start (use Alembic in production)."""
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)


@asynccontextmanager
async def lifespan(app: FastAPI):
    # Startup
    await init_db()
    await kafka_producer.start_kafka_producer()
    logger.info("RTDTS service started.")
    yield
    # Shutdown
    await kafka_producer.stop_kafka_producer()
    await engine.dispose()
    logger.info("RTDTS service stopped.")


app = FastAPI(
    title="Real-Time Driver Tracking System",
    description=(
        "Location Intelligence & Routing · Urban Taxi Ride Pvt. Ltd.\n\n"
        "Captures raw GPS from the driver app, applies a Kalman filter for noise "
        "smoothing, persists to PostgreSQL, and publishes smoothed positions to Kafka "
        "at high throughput for the rider live map, anomaly detection, and feature store."
    ),
    version="1.0.0",
    lifespan=lifespan,
)


# ──────────────────────────────────────────────
# GPS Ingestion
# ──────────────────────────────────────────────

@app.post(
    "/tracking/gps",
    response_model=SmoothedLocationOutput,
    summary="Ingest a raw GPS fix from the driver app",
)
async def ingest_gps(payload: RawGPSInput, db: AsyncSession = Depends(get_db)):
    """
    Core endpoint — called by the driver app on every GPS update interval.
    Applies Kalman smoothing, persists to DB, publishes to Kafka, and returns
    the cleaned coordinates to the caller.
    """
    return await process_gps_fix(db, payload)


# ──────────────────────────────────────────────
# Session Management
# ──────────────────────────────────────────────

@app.post(
    "/tracking/session/start",
    response_model=TrackingSessionResponse,
    summary="Begin a driver tracking session (shift start / trip start)",
)
async def session_start(req: TrackingSessionRequest):
    """
    Initialises a fresh Kalman filter for the driver so stale state from a
    previous session does not pollute the new one.
    """
    reset_filter(req.driver_id)
    logger.info(f"Tracking session started for driver {req.driver_id}")
    return TrackingSessionResponse(
        driver_id=req.driver_id,
        status="started",
        message=f"Tracking session initialised for driver {req.driver_id}.",
    )


@app.post(
    "/tracking/session/stop",
    response_model=TrackingSessionResponse,
    summary="End a driver tracking session (shift end / trip end)",
)
async def session_stop(req: TrackingSessionRequest):
    """
    Resets the Kalman filter state so memory is reclaimed at end of shift.
    """
    reset_filter(req.driver_id)
    logger.info(f"Tracking session stopped for driver {req.driver_id}")
    return TrackingSessionResponse(
        driver_id=req.driver_id,
        status="stopped",
        message=f"Tracking session closed for driver {req.driver_id}.",
    )


# ──────────────────────────────────────────────
# Query
# ──────────────────────────────────────────────

@app.get(
    "/tracking/driver/{driver_id}/latest",
    response_model=LatestLocationResponse,
    summary="Get the most recent smoothed location for a driver",
)
async def latest_location(driver_id: str, db: AsyncSession = Depends(get_db)):
    """
    Returns the last persisted smoothed GPS position for the given driver.
    Useful for dispatch, ETA estimation, and anomaly monitoring.
    """
    result = await get_latest_location(db, driver_id)
    if not result:
        raise HTTPException(status_code=404, detail=f"No location data found for driver {driver_id}")
    return result


# ──────────────────────────────────────────────
# Health
# ──────────────────────────────────────────────

@app.get("/health", summary="Service health check")
def health():
    return {"status": "ok", "service": "rtdts"}
