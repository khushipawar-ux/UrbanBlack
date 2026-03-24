# Urban Black — Route Optimization Engine

Driver-side Route Optimization Engine for the Urban Black ride-hailing platform (Pune).

---

## Project Structure

```
urban_black_engine/
├── main.py                          ← FastAPI app (entry point)
├── config.py                        ← All settings / env vars
├── requirements.txt
├── .env.example                     ← Copy to .env and fill in
│
├── database/
│   ├── connection.py                ← asyncpg connection pool
│   └── queries.py                   ← All SQL queries
│
├── services/
│   ├── google_maps.py               ← Google Maps API wrapper
│   ├── cache.py                     ← Redis cache (+ in-memory fallback)
│   └── route_optimizer.py           ← Core optimization logic
│
├── ml/
│   ├── feature_engineering.py       ← Feature extraction (shared train/infer)
│   ├── train.py                     ← LightGBM training script
│   ├── predictor.py                 ← Model inference singleton
│   └── saved_models/                ← .pkl files go here after training
│
├── models/
│   └── schemas.py                   ← Pydantic request/response models
│
├── data/
│   └── rides_training.csv           ← Place your CSV here
│
└── tests/
    └── test_engine.py               ← Full test suite
```

---

## Setup (Step-by-Step)

### 1. Install dependencies

```bash
cd urban_black_engine
python -m venv venv
source venv/bin/activate        # Windows: venv\Scripts\activate
pip install -r requirements.txt
```

### 2. Configure environment

```bash
cp .env.example .env
# Edit .env — fill in DB_PASSWORD, GOOGLE_MAPS_API_KEY, etc.
```

### 3. Set up the database

**Option A: Docker (Recommended & Easiest)**

```bash
# Start PostgreSQL and Redis in Docker
docker-compose up -d

# Initialize database schema and tables
python database/init_db.py
```

**Option B: Manual PostgreSQL Setup**

```bash
# Create database in PostgreSQL
psql -U postgres -c "CREATE DATABASE urban_black_db;"

# Initialize schema
python database/init_db.py
```

For detailed database setup instructions, see [DATABASE_SETUP.md](DATABASE_SETUP.md)

### 4. Place your training data

```bash
mkdir -p data
cp /path/to/rides_training.csv data/rides_training.csv
```

### 5. Train the ML model

```bash
python ml/train.py \
    --data  data/rides_training.csv \
    --output ml/saved_models/ \
    --version lgbm_v1.0
```

Expected output:
```
===================================================
  MODEL EVALUATION RESULTS
===================================================
  MAE         : 2.8 minutes
  RMSE        : 3.9 minutes
  MAPE        : 7.9 %
  R²          : 0.912
  Within 10%  : 91.2 % of predictions
  ✓ TARGET MET: MAPE ≤ 10% (requirement achieved)
```

### 6. Start the server

```bash
uvicorn main:app --host 0.0.0.0 --port 8000 --reload
```

Open: http://localhost:8000/docs

---

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/health` | System health check |
| GET | `/driver/route` | Full route: driver → pickup → drop |
| GET | `/driver/eta` | ML-corrected ETA only |
| POST | `/driver/reroute` | Mid-trip reroute |
| POST | `/driver/update-location` | Live GPS ingestion |
| POST | `/admin/reload-model` | Hot-reload ML model |

---

## Example Requests

### Get Route
```
GET /driver/route
  ?driver_lat=18.5362&driver_lng=73.8935
  &pickup_lat=18.5309&pickup_lng=73.8474
  &drop_lat=18.5912&drop_lng=73.7389
  &driver_id=<uuid>&ride_id=<uuid>
```

Response:
```json
{
  "polyline": "yvpnBiotrM...",
  "distance_km": 14.2,
  "google_eta_min": 42.0,
  "ml_eta_min": 46.8,
  "eta_correction_min": 4.8,
  "traffic_condition": "heavy",
  "is_peak_hour": true,
  "served_from_cache": false,
  "response_time_ms": 145,
  "steps": [
    { "instruction": "Head west on North Main Road", "distance_meters": 300, "duration_seconds": 45 },
    { "instruction": "Turn left onto MG Road", "distance_meters": 1200, "duration_seconds": 180 }
  ]
}
```

### Mid-Trip Reroute
```json
POST /driver/reroute
{
  "ride_id": "uuid",
  "driver_id": "uuid",
  "current_lat": 18.5550,
  "current_lng": 73.8200,
  "drop_lat": 18.5912,
  "drop_lng": 73.7389,
  "current_eta_min": 35.0,
  "trigger": "traffic_change"
}
```

---

## Architecture Flow

```
Driver App
    │
    ▼
FastAPI Server (main.py)
    │
    ├── Redis Cache ──── (cache hit → return immediately)
    │       │
    │       └── cache miss ──►
    │
    ├── Google Directions API
    │       │ polyline, distance, base ETA
    │       ▼
    ├── LightGBM ML Model  ←── trained on rides_training.csv
    │       │ corrected ETA (±2-5 min Pune-specific)
    │       ▼
    └── RouteResponse (JSON)
            │
            ▼
        Driver App renders on Google Maps embed
```

---

## Running Tests

```bash
pytest tests/ -v
```

---

## Retraining the Model

Run this weekly (or after 10K+ new rides):

```bash
python ml/train.py \
    --data  data/rides_training.csv \
    --output ml/saved_models/ \
    --version lgbm_v1.1
```

Then hot-reload without restarting the server:

```bash
curl -X POST "http://localhost:8000/admin/reload-model?model_path=ml/saved_models/eta_lgbm_v1.1.pkl"
```

---

## Without a Google Maps API Key

The engine runs in **mock mode** automatically when `GOOGLE_MAPS_API_KEY` is not set.
Mock mode returns realistic-shaped responses using Haversine distance + Pune speed estimates.
This is useful for local development and unit tests.
