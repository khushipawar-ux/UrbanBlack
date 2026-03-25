# Real-Time Driver Tracking System (RTDTS) — LIR-02

**Location Intelligence & Routing · Driver App · Urban Taxi Ride Pvt. Ltd.**

A continuous background service that captures raw GPS from the driver app, applies a **Kalman filter** to smooth urban-interference noise, persists cleaned coordinates to PostgreSQL, and publishes every update to Kafka for downstream consumers.

---

## Architecture

```
Driver App GPS Fix
       │
  POST /tracking/gps
       │
  ┌────▼─────────────┐
  │  Kalman Filter   │  ← 2D constant-velocity model (numpy)
  │  (per-driver)    │     smooths multipath & network jitter
  └────┬─────────────┘
       │ smoothed_lat / smoothed_lng
  ┌────▼──────────────────────────────────┐
  │         location_service.py           │
  │  1. Persist → driver_locations        │
  │  2. Persist → lir_trip_gps_trail      │
  │  3. Publish → Kafka                   │
  └────┬──────────────────────────────────┘
       │
  Kafka topic: driver_location_updates
       │
  ┌────┼──────────────────────────────────┐
  │    │   Downstream Consumers           │
  │  Rider Live Map                       │
  │  Geo-Spatial Anomaly Detection (LGAD) │
  │  Feature Store                        │
  └───────────────────────────────────────┘
```

---

## Features

- **Kalman GPS Smoothing** — 2D constant-velocity Kalman filter removes urban multipath, signal jitter, and tall-building interference.
- **High-Throughput Kafka Publishing** — Publishes to `driver_location_updates` topic, feeding the rider live map, anomaly model, and feature store simultaneously.
- **PostgreSQL Persistence** — Writes raw + smoothed coordinates to `driver_locations`; records sequential trail in `lir_trip_gps_trail` during active rides.
- **Session Management** — `/tracking/session/start` and `/tracking/session/stop` initialise / reset the per-driver Kalman filter state.

---

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/tracking/gps` | Ingest a raw GPS fix, returns smoothed coordinates |
| `POST` | `/tracking/session/start` | Initialise Kalman filter for a driver (shift start) |
| `POST` | `/tracking/session/stop` | Reset Kalman state (shift end) |
| `GET`  | `/tracking/driver/{id}/latest` | Latest smoothed location for a driver |
| `GET`  | `/health` | Service health check |

Interactive docs at → `http://localhost:8001/docs`

---

## Kafka Topic Schema — `driver_location_updates`

```json
{
  "driver_id": "EMP-001",
  "smoothed_lat": 18.531234,
  "smoothed_lng": 73.844512,
  "bearing": 142.5,
  "speed_kmh": 38.0,
  "is_on_trip": true,
  "ride_id": "ride-uuid-xxx",
  "location_id": "db-row-uuid-xxx",
  "timestamp": "2026-03-24T09:45:00+05:30"
}
```

---

## Setup

### 1. Install dependencies
```bash
pip install -r requirements.txt
```

### 2. Configure environment
Copy `.env` and fill in values:
```
DB_HOST=localhost
DB_PORT=5432
DB_USER=postgres
DB_PASSWORD=root
DB_NAME=urbanblack

KAFKA_BOOTSTRAP_SERVERS=localhost:9092
KAFKA_TOPIC_LOCATION_UPDATES=driver_location_updates

KALMAN_PROCESS_NOISE=0.01
KALMAN_MEASUREMENT_NOISE=5.0
GPS_UPDATE_INTERVAL_SEC=3
```

### 3. Start infrastructure (Docker)
```bash
docker-compose up -d
```

### 4. Run the service
```bash
uvicorn main:app --reload --host 0.0.0.0 --port 8001
```

---

## Downstream Integration

| Consumer | What it uses |
|----------|-------------|
| **Rider live map** | `smoothed_lat`, `smoothed_lng` for stable driver pin |
| **Geo-Spatial Anomaly Detection** | Position stream for detour / stop / spoof detection |
| **Feature Store** | Real-time driver location features for ETA & routing models |
| **RiderRouteOpt (LIR-01)** | Reads `driver_locations` for live route adjustment |
