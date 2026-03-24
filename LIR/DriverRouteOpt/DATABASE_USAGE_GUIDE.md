# Urban Black — Database Usage Guide

## Quick Start: Check If Database is Working

### Run the Test:
```bash
python test_database_connection.py
```

This will check:
✓ PostgreSQL connectivity  
✓ Database exists  
✓ Tables are created  
✓ Write/read operations work  

---

## Architecture Overview

### Connection Pool (Singleton Pattern)

```python
# Location: database/connection.py
_pool: Optional[asyncpg.Pool] = None

async def create_pool():
    """Initialize asyncpg connection pool (min: 2, max: 10 connections)"""
    _pool = await asyncpg.create_pool(
        host=settings.DB_HOST,          # localhost
        port=settings.DB_PORT,          # 5432
        database=settings.DB_NAME,      # urban_black_db
        user=settings.DB_USER,          # postgres
        password=settings.DB_PASSWORD,  # admin123
        min_size=2,                     # minimum connections
        max_size=10,                    # maximum connections
        command_timeout=30,             # 30-second query timeout
    )
```

**Key Features:**
- **Lazy initialization**: Pool created on first `get_pool()` call
- **Singleton**: Shared across entire application
- **Connection reuse**: Reduces overhead of opening new connections
- **Graceful shutdown**: `close_pool()` called on app exit

---

## Configuration: Where Credentials Come From

### File: `.env` (in project root)
```
DB_HOST=localhost
DB_PORT=5432
DB_NAME=urban_black_db
DB_USER=postgres
DB_PASSWORD=admin123
DB_POOL_MIN=2
DB_POOL_MAX=10
```

### File: `config.py`
```python
class Settings:
    DB_HOST: str      = os.getenv("DB_HOST", "localhost")      # from .env
    DB_PORT: int      = int(os.getenv("DB_PORT", 5432))        # from .env
    DB_NAME: str      = os.getenv("DB_NAME", "urban_black_db")
    DB_USER: str      = os.getenv("DB_USER", "postgres")
    DB_PASSWORD: str  = os.getenv("DB_PASSWORD", "postgres")
```

**Loading order:**
1. Read `.env` file via `python-dotenv`
2. Use environment variables as fallback
3. Use hardcoded defaults as last resort

---

## Database Tables (8 Tables)

### 1. **drivers** — Driver profiles & live location
```sql
CREATE TABLE drivers (
    driver_id UUID PRIMARY KEY,
    full_name VARCHAR(255),
    email VARCHAR(255),
    phone VARCHAR(20),
    rating FLOAT DEFAULT 4.5,
    current_lat FLOAT,      -- Updated in real-time
    current_lng FLOAT,      -- Updated in real-time
    last_location_update TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT NOW()
);
```

**Usage:** Store driver info, track live location

### 2. **vehicles** — Linked to drivers
```sql
CREATE TABLE vehicles (
    vehicle_id UUID PRIMARY KEY,
    driver_id UUID REFERENCES drivers(driver_id),
    vehicle_type VARCHAR(50),      -- 'sedan', 'suv', 'premium'
    license_plate VARCHAR(50),
    color VARCHAR(50),
    is_ac BOOLEAN DEFAULT TRUE
);
```

### 3. **eta_predictions** — ML prediction logging (time-series)
```sql
CREATE TABLE eta_predictions (
    prediction_id SERIAL PRIMARY KEY,
    ride_id UUID,
    driver_id UUID,
    model_version VARCHAR(50),     -- 'lgbm_v1', 'sklearn_v2', etc
    distance_km FLOAT,
    google_eta_min FLOAT,          -- Google's estimate
    ml_eta_min FLOAT,              -- LightGBM correction
    eta_correction_min FLOAT,      -- Difference
    confidence_score FLOAT,
    predicted_at TIMESTAMP DEFAULT NOW()
);
CREATE INDEX idx_eta_pred_ride ON eta_predictions(ride_id);
Create Index idx_eta_pred_driver ON eta_predictions(driver_id);
```

**Usage:** Log every ML prediction for analytics & model training

### 4. **reroute_events** — Reroute decisions
```sql
CREATE TABLE reroute_events (
    reroute_id SERIAL PRIMARY KEY,
    ride_id UUID,
    driver_id UUID,
    triggered_at TIMESTAMP,
    trigger_reason VARCHAR(255),   -- 'ml_suggestion', 'accident', etc
    old_eta_min FLOAT,
    new_eta_min FLOAT,
    time_saved_min FLOAT,          -- Calculated difference
    response_ms INT                -- API response time
);
```

### 5. **rides** — Historical ride data (for ML training)
```sql
CREATE TABLE rides (
    ride_id UUID PRIMARY KEY,
    driver_id UUID,
    pickup_lat FLOAT, pickup_lng FLOAT,
    drop_lat FLOAT, drop_lng FLOAT,
    distance_km FLOAT,
    actual_duration_min FLOAT,     -- Ground truth for ML
    traffic_congestion_score FLOAT,
    hour_of_day INT,               -- Features
    day_of_week INT,
    ride_status VARCHAR(50)        -- 'completed', 'cancelled'
);
CREATE INDEX idx_rides_zone_pair 
ON rides(pickup_zone_id, drop_zone_id, hour_of_day, day_of_week);
```

### 6. **traffic_patterns** — Historical traffic by segment/time
```sql
CREATE TABLE traffic_patterns (
    pattern_id SERIAL PRIMARY KEY,
    segment_id INT,                -- Road segment ID
    hour_of_day INT,               -- 0-23
    day_of_week INT,               -- 0-6 (Mon-Sun)
    avg_speed_kmph FLOAT,
    congestion_level VARCHAR(50),  -- 'light', 'moderate', 'heavy'
    avg_delay_min_per_km FLOAT,
    vehicle_count INT
);
```

### 7. **route_cache** — Google Directions caching
```sql
CREATE TABLE route_cache (
    cache_id SERIAL PRIMARY KEY,
    cache_key VARCHAR(255) UNIQUE,
    origin_lat FLOAT, origin_lng FLOAT,
    destination_lat FLOAT, destination_lng FLOAT,
    route_polyline TEXT,           -- Encoded polyline
    distance_km FLOAT,
    google_duration_sec INT,
    google_eta_traffic_sec INT,
    turn_by_turn_steps JSONB,      -- JSON array of directions
    cached_at TIMESTAMP,
    expires_at TIMESTAMP,          -- TTL expiration
    hit_count INT DEFAULT 1        -- Cache hits counter
);
```

### 8. **model_versions** — ML model metadata
```sql
CREATE TABLE model_versions (
    version_tag VARCHAR(50) PRIMARY KEY,
    algorithm VARCHAR(100),        -- 'LightGBM', 'sklearn'
    trained_at TIMESTAMP,
    is_active BOOLEAN DEFAULT FALSE,
    metrics JSONB                  -- {mae: 2.8, mape: 7.9, r2: 0.912}
);
```

---

## How to Use: Query Patterns

### Pattern 1: Basic Query (Single Row)
```python
# File: database/queries.py
async def get_driver(driver_id: str):
    sql = "SELECT * FROM drivers WHERE driver_id = $1::uuid"
    pool = await get_pool()
    async with pool.acquire() as conn:
        driver = await conn.fetchrow(sql, driver_id)
        return driver
```

**Key points:**
- `$1, $2, $3` — Parameterized queries (SQL injection safe)
- `fetchrow()` — Returns single row
- `pool.acquire()` — Get connection from pool
- `async with` — Auto-cleanup connection

### Pattern 2: Insert with RETURNING
```python
async def log_eta_prediction(ride_id, driver_id, ml_eta_min):
    sql = """
        INSERT INTO eta_predictions (ride_id, driver_id, ml_eta_min, predicted_at)
        VALUES ($1::uuid, $2::uuid, $3, NOW())
        RETURNING prediction_id
    """
    pool = await get_pool()
    async with pool.acquire() as conn:
        row = await conn.fetchrow(sql, ride_id, driver_id, ml_eta_min)
        return row["prediction_id"]  # Get generated ID
```

### Pattern 3: Update Operation
```python
async def update_driver_location(driver_id: str, lat: float, lng: float):
    sql = """
        UPDATE drivers 
        SET current_lat = $1, current_lng = $2, last_location_update = NOW()
        WHERE driver_id = $3::uuid
    """
    pool = await get_pool()
    async with pool.acquire() as conn:
        await conn.execute(sql, lat, lng, driver_id)
```

### Pattern 4: Multiple Rows (Fetch All)
```python
async def get_nearby_drivers(lat: float, lng: float, radius_km: float):
    sql = """
        SELECT driver_id, full_name, rating, current_lat, current_lng,
               (6371 * acos(...Haversine formula...)) AS distance_km
        FROM drivers
        WHERE is_online = TRUE
        HAVING distance_km < $1
        ORDER BY distance_km ASC
    """
    pool = await get_pool()
    async with pool.acquire() as conn:
        rows = await conn.fetch(sql, radius_km, lat, lng)
        return [dict(row) for row in rows]  # Convert to dict for JSON
```

### Pattern 5: Scalar Query (Single Value)
```python
async def count_drivers():
    pool = await get_pool()
    async with pool.acquire() as conn:
        count = await conn.fetchval("SELECT COUNT(*) FROM drivers")
        return count
```

### Pattern 6: Transaction (Multiple Operations)
```python
async def complete_ride(ride_id: str, actual_duration: float):
    pool = await get_pool()
    async with pool.acquire() as conn:
        async with conn.transaction():  # Or: transaction rollback on error
            # Update ride status
            await conn.execute(
                "UPDATE rides SET ride_status = 'completed', actual_duration_min = $1 WHERE ride_id = $2::uuid",
                actual_duration, ride_id
            )
            # Update driver location
            await conn.execute(
                "UPDATE drivers SET is_online = FALSE WHERE driver_id = $1::uuid",
                driver_id
            )
            # Both succeed or both rollback
```

---

## Health Checks: Testing Database

### 1. **In-Code Health Check**
```python
# Location: database/connection.py
async def check_connection() -> bool:
    """Ping the database."""
    try:
        pool = await get_pool()
        async with pool.acquire() as conn:
            await conn.fetchval("SELECT 1")
        return True
    except Exception as e:
        log.warning("DB health check failed", error=str(e))
        return False
```

### 2. **API Health Endpoint**
```python
# Location: main.py
@app.get("/health")
async def health_check():
    db_ok = await check_connection()
    redis_ok = await cache.is_healthy()
    model_ok = predictor.is_loaded
    
    return {
        "status": "healthy" if all([db_ok, redis_ok, model_ok]) else "degraded",
        "db_connected": db_ok,
        "redis_connected": redis_ok,
        "model_loaded": model_ok,
    }
```

**Usage:**
```bash
# When server is running:
curl http://localhost:8000/health

# Response:
{
  "status": "healthy",
  "db_connected": true,
  "redis_connected": true,
  "model_loaded": true
}
```

### 3. **Manual Test Script**
```bash
python test_database_connection.py
```

---

## Integration in Application

### **Application Startup** (`main.py`)
```python
@asynccontextmanager
async def lifespan(app: FastAPI):
    """Run at startup."""
    log.info("Starting...")
    
    # 1. Connect to database
    await create_pool()  # Initialize connection pool
    log.info("Database pool initialized")
    
    # 2. Connect to Redis
    cache = get_cache()
    await cache.connect()
    
    # 3. Load ML model
    predictor = get_predictor()
    predictor.load()
    
    yield  # ← App runs here
    
    # Shutdown
    await close_pool()
    await cache.disconnect()
    log.info("Shutdown complete")

app = FastAPI(lifespan=lifespan)
```

### **Route Optimizer Usage** (`services/route_optimizer.py`)
```python
async def get_route(self, driver_lat, driver_lng, ...):
    # 1. Check cache
    cached = await self.cache.get_route(...)
    
    if cached:
        # Log prediction (fire-and-forget)
        await self._log_prediction_async(ride_id, driver_id, ml_result, ...)
        return self._build_route_response(...)
    
    # 2. Call Google Directions API
    directions = self.gmaps.get_directions(...)
    
    # 3. Run ML model
    ml_result = self._run_ml(...)
    
    # 4. Log to database asynchronously
    await self._log_prediction_async(ride_id, driver_id, ml_result, ...)
    
    # 5. Cache result
    await self.cache.set_route(...)
    await self._save_db_cache_async(...)  # Mirror to DB
    
    return self._build_route_response(...)
```

---

## Error Handling

### **Graceful Degradation**
If database is down, the app continues with fallbacks:

```python
# If DB write fails, log warning but don't crash
try:
    await log_eta_prediction(...)
except Exception as e:
    log.warning("DB logging failed (non-critical)", error=str(e))
    # Continue anyway
```

### **Connection Errors**
```python
try:
    conn = await asyncpg.connect(...)
except asyncpg.InvalidPasswordError:
    log.error("Invalid database password")
except asyncpg.CannotConnectNowError:
    log.error("PostgreSQL is not running")
except asyncpg.ConnectionDoesNotExistError:
    log.error("Database does not exist — run: python database/init_db.py")
```

---

## Troubleshooting

### Problem: "Connection refused"
**Solution:** PostgreSQL is not running
```bash
# Windows (if installed locally)
# Services → Start "PostgreSQL"

# Or use Docker:
docker-compose up -d
```

### Problem: "FATAL: password authentication failed"
**Solution:** Wrong password in `.env`
```bash
# Check DB_PASSWORD in .env matches PostgreSQL setup
```

### Problem: "database 'urban_black_db' does not exist"
**Solution:** Initialize database
```bash
python database/init_db.py
```

### Problem: "relation 'drivers' does not exist"
**Solution:** Tables not created
```bash
python database/init_db.py
```

---

## Summary Checklist

✅ **Database Setup:**
- [ ] PostgreSQL installed and running
- [ ] `.env` file with correct credentials
- [ ] `python database/init_db.py` executed (tables created)

✅ **Testing:**
- [ ] `python test_database_connection.py` passes
- [ ] `/health` endpoint shows `db_connected: true`
- [ ] Tables visible in test output

✅ **Application:**
- [ ] `python main.py` starts without DB errors
- [ ] API endpoints return data
- [ ] Database logs in `/health` endpoint

✅ **Usage:**
- [ ] Use patterns from `database/queries.py` as templates
- [ ] Always follow `async with pool.acquire() as conn:` pattern
- [ ] Use parameterized queries: `$1, $2, $3`
- [ ] Catch and log exceptions gracefully

---

## Quick Command Reference

```bash
# Test database connection
python test_database_connection.py

# Initialize database (one-time)
python database/init_db.py

# Start API server
python main.py

# Check health
curl http://localhost:8000/health

# Access API docs
http://localhost:8000/docs
```

---

**Your database is now ready to use!** 🚀

For integration examples, see [database/queries.py](database/queries.py) and [services/route_optimizer.py](services/route_optimizer.py)
