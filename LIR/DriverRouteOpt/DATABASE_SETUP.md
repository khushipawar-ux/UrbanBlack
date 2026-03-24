# Urban Black — Database Setup Guide

## Quick Setup (Docker — Recommended)

### Prerequisites
- Docker and Docker Compose installed on your system

### Step 1: Start PostgreSQL and Redis
```bash
docker-compose up -d
```

This will:
- Create and start a PostgreSQL 15 container on port 5432
- Create and start a Redis container on port 6379
- Both services will automatically start on system boot

### Step 2: Initialize Database Schema
```bash
python database/init_db.py
```

This will:
- Create the `urban_black_db` database
- Create all required tables (drivers, vehicles, eta_predictions, etc.)
- Set up indexes for performance

### Step 3: Verify Everything Works
```bash
python main.py
```

Visit `http://localhost:8000/health` to verify all systems are running.

---

## Manual Setup (Without Docker)

### Prerequisites
- PostgreSQL 12+ installed locally
- Python 3.9+

### Step 1: Create PostgreSQL Database
```bash
# Connect as postgres user
psql -U postgres

# Inside psql, run:
CREATE DATABASE urban_black_db;
```

### Step 2: Set PostgreSQL Password (if needed)
```bash
# Inside psql:
ALTER USER postgres WITH PASSWORD 'admin123';
```

### Step 3: Install Redis
- Download Redis from https://github.com/microsoftarchive/redis/releases
- Or use Windows Subsystem for Linux (WSL)

### Step 4: Update .env File
Make sure your `.env` has correct database credentials:
```
DB_HOST=localhost
DB_PORT=5432
DB_NAME=urban_black_db
DB_USER=postgres
DB_PASSWORD=admin123
REDIS_HOST=localhost
REDIS_PORT=6379
```

### Step 5: Initialize Database Schema
```bash
python database/init_db.py
```

### Step 6: Start PostgreSQL and Redis Services
- Start PostgreSQL service
- Start Redis server
- Then: `python main.py`

---

## Troubleshooting

### PostgreSQL Connection Error
```
psycopg2.OperationalError: could not connect to server
```
**Solution:** 
1. Verify PostgreSQL is running: `docker-compose ps`
2. Check credentials in `.env` file match Docker setup
3. Ensure port 5432 is not in use by another application

### asyncpg Connection Error
```
asyncpg.exceptions.ClientError: no password supplied
```
**Solution:**
1. Verify `DB_PASSWORD` is set in `.env`
2. Make sure `.env` file exists (not just `.env.example`)
3. Restart the application after changing `.env`

### Cannot Import asyncpg
```
ModuleNotFoundError: No module named 'asyncpg'
```
**Solution:**
```bash
pip install -r requirements.txt
```

### Docker Issues
```bash
# Stop all containers
docker-compose down

# Start fresh
docker-compose up -d

# View logs
docker-compose logs -f
```

---

## Database Schema Overview

### Tables Created
1. **drivers** — Driver information and current location
2. **vehicles** — Vehicle details linked to drivers
3. **eta_predictions** — ML model predictions and corrections
4. **reroute_events** — All reroute decisions logged
5. **rides** — Historical ride data for ML training
6. **traffic_patterns** — Segment-level traffic data
7. **route_cache** — Cached routes for quick lookup
8. **model_versions** — ML model metadata and status

### Indexes
Optimized queries for:
- Ride lookups by driver/ride ID
- Zone-pair traffic patterns
- Route cache hits
- Time-series analysis

---

## Backup and Restore

### Backup Database
```bash
docker exec urban_black_postgres pg_dump -U postgres urban_black_db > backup.sql
```

### Restore from Backup
```bash
docker exec -i urban_black_postgres psql -U postgres urban_black_db < backup.sql
```

---

## Development vs Production

### Development (Current Setup)
- Local PostgreSQL in Docker
- Redis cache enabled
- ML model in mock mode (if API key not set)
- API logs to stdout

### Production
- Remote PostgreSQL (AWS RDS, etc.)
- ElastiCache or managed Redis
- Latest validated ML model
- Structured logging to CloudWatch/ELK
- SSL/TLS for all connections

Update these in `.env` and environment variables before deploying.

---

## Next Steps

1. ✓ Database created and initialized
2. ✓ Tables and indexes set up
3. Get Google Maps API key from [console.cloud.google.com](https://console.cloud.google.com)
4. Update `GOOGLE_MAPS_API_KEY` in `.env`
5. Run: `python main.py`
6. Open http://localhost:8000/docs for API documentation
