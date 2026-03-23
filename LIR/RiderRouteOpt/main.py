from fastapi import FastAPI, Depends, BackgroundTasks
from sqlalchemy.ext.asyncio import AsyncSession
import asyncio
from contextlib import asynccontextmanager

from database import engine, Base, get_db
from schemas import PreTripPreviewRequest, RoutePreviewResponse, LiveRouteRequest, LiveRouteResponse
from services.routing_engine import compute_pre_trip_route, compute_live_route
from services.kafka_consumer import start_kafka_consumer

# Create tables for fresh setups (in prod, use alembic)
async def init_db():
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)

@asynccontextmanager
async def lifespan(app: FastAPI):
    # Startup
    await init_db()
    kafka_task = asyncio.create_task(start_kafka_consumer())
    yield
    # Shutdown
    kafka_task.cancel()
    await engine.dispose()

app = FastAPI(title="LIR Route Optimization Engine", lifespan=lifespan)

@app.post("/route/preview", response_model=RoutePreviewResponse)
async def route_preview(req: PreTripPreviewRequest, db: AsyncSession = Depends(get_db)):
    """ Serves the pre-trip preview on the booking screen """
    return await compute_pre_trip_route(db, req)

@app.post("/route/live", response_model=LiveRouteResponse)
async def route_live(req: LiveRouteRequest, db: AsyncSession = Depends(get_db)):
    """ Polled during active trip to get live route card updates """
    return await compute_live_route(db, req)
    
@app.get("/health")
def health():
    return {"status": "ok"}
