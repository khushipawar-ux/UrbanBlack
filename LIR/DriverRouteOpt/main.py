"""
main.py — Urban Black Route Optimization Engine
FastAPI application entry point.

Endpoints:
    GET  /health                   → System health check
    GET  /driver/route             → Full route: driver → pickup → drop
    GET  /driver/eta               → ETA only (lightweight)
    POST /driver/reroute           → Mid-trip reroute
    POST /driver/update-location   → Live GPS update from driver
    POST /admin/reload-model       → Hot-reload ML model without restart

Run:
    uvicorn main:app --host 0.0.0.0 --port 8000 --reload
"""
import os
from dotenv import load_dotenv

load_dotenv(dotenv_path="C:/Users/GCS/Downloads/urban_black_engine/.env")

print("KEY CHECK:", os.getenv("GOOGLE_MAPS_API_KEY"))
API_KEY = os.getenv("GOOGLE_MAPS_API_KEY")
import time
import structlog
from contextlib import asynccontextmanager
from datetime import datetime

from fastapi import FastAPI, HTTPException, Query, Depends, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse

from config import settings
from models.schemas import (
    RouteRequest,
    ETARequest,
    RerouteRequest,
    LocationUpdateRequest,
    RouteResponse,
    ETAResponse,
    RerouteResponse,
    LocationUpdateResponse,
    HealthResponse,
)
from services.route_optimizer import get_optimizer
from services.cache import get_cache
from database.connection import create_pool, close_pool, check_connection
from database.queries import update_driver_location
from ml.predictor import get_predictor

log = structlog.get_logger(__name__)


# ─────────────────────────────────────────────────────────────────────────────
# LIFESPAN (startup + shutdown)
# ─────────────────────────────────────────────────────────────────────────────

@asynccontextmanager
async def lifespan(app: FastAPI):
    """Run startup tasks before yielding, shutdown tasks after."""
    log.info("Starting Urban Black Route Optimization Engine",
             version=settings.APP_VERSION,
             env=settings.ENV)

    # 1. Connect to database
    try:
        await create_pool()
        log.info("Database pool initialized")
    except Exception as e:
        log.error("Database connection failed", error=str(e))

    # 2. Connect to Redis
    cache = get_cache()
    await cache.connect()

    # 3. Load ML model
    predictor = get_predictor()
    if predictor.is_loaded:
        log.info("ML model loaded", version=predictor.model_version)
    else:
        log.warning("ML model not loaded — using heuristic fallback")

    # 4. Warm up route optimizer
    optimizer = get_optimizer()
    log.info("Route optimizer ready")

    yield  # ← app runs here

    # Shutdown
    log.info("Shutting down...")
    await close_pool()
    await cache.disconnect()
    log.info("Shutdown complete")


# ─────────────────────────────────────────────────────────────────────────────
# APP SETUP
# ─────────────────────────────────────────────────────────────────────────────

app = FastAPI(
    title=settings.APP_NAME,
    version=settings.APP_VERSION,
    description=(
        "Driver-side Route Optimization Engine for Urban Black ride-hailing platform. "
        "Provides ML-corrected ETA, optimal routes, and real-time rerouting for Pune."
    ),
    lifespan=lifespan,
    docs_url="/docs",
    redoc_url="/redoc",
)

# CORS — allow driver app and admin dashboard
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],      # restrict in production
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


# ─────────────────────────────────────────────────────────────────────────────
# MIDDLEWARE — latency logging
# ─────────────────────────────────────────────────────────────────────────────

@app.middleware("http")
async def log_request_time(request: Request, call_next):
    t_start = time.monotonic()
    response = await call_next(request)
    elapsed_ms = int((time.monotonic() - t_start) * 1000)
    response.headers["X-Response-Time-Ms"] = str(elapsed_ms)

    if elapsed_ms > settings.MAX_ROUTE_RESPONSE_MS:
        log.warning("Slow response",
                    path=request.url.path,
                    method=request.method,
                    ms=elapsed_ms,
                    limit_ms=settings.MAX_ROUTE_RESPONSE_MS)
    return response


# ─────────────────────────────────────────────────────────────────────────────
# ROUTES
# ─────────────────────────────────────────────────────────────────────────────

@app.get("/health", response_model=HealthResponse, tags=["System"])
async def health_check():
    """
    System health check.
    Verifies DB, Redis, Google Maps, and ML model are all operational.
    """
    predictor = get_predictor()
    cache     = get_cache()
    gmaps     = get_optimizer().gmaps

    db_ok     = await check_connection()
    redis_ok  = await cache.is_healthy()
    gmaps_ok  = gmaps.is_available()

    status = "healthy" if (db_ok and redis_ok and predictor.is_loaded) else "degraded"

    return HealthResponse(
        status=status,
        version=settings.APP_VERSION,
        model_loaded=predictor.is_loaded,
        db_connected=db_ok,
        redis_connected=redis_ok,
        google_maps_ok=gmaps_ok,
    )


@app.get(
    "/driver/route",
    response_model=RouteResponse,
    tags=["Driver"],
    summary="Get optimized route for a ride",
    description=(
        "Returns the optimal route from driver's current location → pickup → drop. "
        "Includes ML-corrected ETA, encoded polyline, and turn-by-turn steps."
    ),
)
async def get_driver_route(
    driver_lat:  float = Query(..., description="Driver's current latitude",  example=18.5362),
    driver_lng:  float = Query(..., description="Driver's current longitude", example=73.8935),
    pickup_lat:  float = Query(..., description="Rider pickup latitude",       example=18.5309),
    pickup_lng:  float = Query(..., description="Rider pickup longitude",      example=73.8474),
    drop_lat:    float = Query(..., description="Rider drop latitude",         example=18.5912),
    drop_lng:    float = Query(..., description="Rider drop longitude",        example=73.7389),
    driver_id:   str   = Query(None, description="Driver UUID for logging"),
    ride_id:     str   = Query(None, description="Ride UUID for logging"),
):
    """
    **Main endpoint for the driver app.**

    Flow:
    1. Check Redis cache (5-min TTL)
    2. Call Google Directions API (if cache miss)
    3. Apply LightGBM ETA correction
    4. Return route + ML ETA to driver app
    """
    try:
        optimizer = get_optimizer()
        result = await optimizer.get_route(
            driver_lat=driver_lat, driver_lng=driver_lng,
            pickup_lat=pickup_lat, pickup_lng=pickup_lng,
            drop_lat=drop_lat,     drop_lng=drop_lng,
            driver_id=driver_id,   ride_id=ride_id,
        )
        return result
    except ValueError as e:
        raise HTTPException(status_code=422, detail=str(e))
    except RuntimeError as e:
        raise HTTPException(status_code=503, detail=str(e))
    except Exception as e:
        log.error("Unexpected error in /driver/route", error=str(e))
        raise HTTPException(status_code=500, detail="Internal server error")


@app.get(
    "/driver/eta",
    response_model=ETAResponse,
    tags=["Driver"],
    summary="Get ML-corrected ETA only",
    description="Lighter than /driver/route — returns only the ETA with no polyline or steps.",
)
async def get_eta(
    origin_lat: float = Query(..., example=18.5362),
    origin_lng: float = Query(..., example=73.8935),
    dest_lat:   float = Query(..., example=18.5912),
    dest_lng:   float = Query(..., example=73.7389),
    driver_id:  str   = Query(None),
    ride_id:    str   = Query(None),
):
    try:
        optimizer = get_optimizer()
        return await optimizer.get_eta(
            origin_lat=origin_lat, origin_lng=origin_lng,
            dest_lat=dest_lat,     dest_lng=dest_lng,
            driver_id=driver_id,   ride_id=ride_id,
        )
    except Exception as e:
        log.error("Error in /driver/eta", error=str(e))
        raise HTTPException(status_code=500, detail="ETA calculation failed")


@app.post(
    "/driver/reroute",
    response_model=RerouteResponse,
    tags=["Driver"],
    summary="Reroute mid-trip",
    description=(
        "Called when the driver deviates >200m or traffic changes significantly. "
        "Returns a new route if it saves ≥5% travel time."
    ),
)
async def reroute_driver(request: RerouteRequest):
    """
    Mid-trip rerouting.

    The driver app should call this every 30 seconds. The engine checks
    whether a better route exists. If improvement < 5%, returns rerouted=False.
    """
    try:
        optimizer = get_optimizer()
        return await optimizer.reroute(
            ride_id=request.ride_id,
            driver_id=request.driver_id,
            current_lat=request.current_lat,
            current_lng=request.current_lng,
            drop_lat=request.drop_lat,
            drop_lng=request.drop_lng,
            current_eta_min=request.current_eta_min,
            trigger=request.trigger.value,
        )
    except Exception as e:
        log.error("Error in /driver/reroute", error=str(e))
        raise HTTPException(status_code=500, detail="Reroute calculation failed")


@app.post(
    "/driver/update-location",
    response_model=LocationUpdateResponse,
    tags=["Driver"],
    summary="Receive live GPS from driver",
    description=(
        "Driver app sends GPS every 5 seconds. "
        "Engine updates driver position in DB and checks if reroute is needed."
    ),
)
async def update_location(request: LocationUpdateRequest):
    """
    Live GPS ingestion from driver app.
    Updates driver's current_lat/current_lng in the drivers table.
    """
    try:
        updated = await update_driver_location(
            driver_id=request.driver_id,
            latitude=request.latitude,
            longitude=request.longitude,
        )
        return LocationUpdateResponse(
            driver_id=request.driver_id,
            received=True,
            needs_reroute=False,   # reroute check handled client-side every 30s
            message="Location updated" if updated else "DB write skipped (non-critical)",
        )
    except Exception as e:
        log.warning("Location update failed", driver_id=request.driver_id, error=str(e))
        # Return success anyway — GPS logging failures shouldn't break the driver app
        return LocationUpdateResponse(
            driver_id=request.driver_id,
            received=True,
            message="Received (logging failed)",
        )


@app.post(
    "/admin/reload-model",
    tags=["Admin"],
    summary="Hot-reload ML model",
    description="Reload the LightGBM model from disk without restarting the server.",
)
async def reload_model(model_path: str = Query(None)):
    """Hot-swap the ML model. Useful after retraining."""
    predictor = get_predictor()
    success   = predictor.reload(model_path)
    if success:
        return {
            "status":  "success",
            "message": f"Model reloaded: {predictor.model_version}",
            "loaded":  predictor.is_loaded,
        }
    return JSONResponse(
        status_code=500,
        content={"status": "failed", "message": "Model reload failed — old model retained"},
    )


@app.get("/", tags=["System"])
async def root():
    return {
        "service":    settings.APP_NAME,
        "version":    settings.APP_VERSION,
        "status":     "running",
        "docs":       "/docs",
        "health":     "/health",
        "city":       settings.CITY_NAME,
        "timestamp":  datetime.now().isoformat(),
    }


# ─────────────────────────────────────────────────────────────────────────────
# RUN DIRECTLY
# ─────────────────────────────────────────────────────────────────────────────

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "main:app",
        host=settings.HOST,
        port=settings.PORT,
        reload=settings.ENV == "development",
        log_level="info",
    )
