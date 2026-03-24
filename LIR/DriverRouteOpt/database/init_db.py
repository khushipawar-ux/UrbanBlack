"""
database/init_db.py — Initialize PostgreSQL database and schema for Urban Black
Run this once to set up the database.

Usage:
    python database/init_db.py
"""

import asyncpg
import sys
from pathlib import Path

# Connection parameters
DB_HOST = "localhost"
DB_PORT = 5432
DB_USER = "postgres"
DB_PASSWORD = "admin123"
DB_NAME = "urban_black_db"
DB_ADMIN = "postgres"  # default admin user


async def create_database():
    """Create the database if it doesn't exist."""
    try:
        # Connect as postgres admin to create DB
        conn = await asyncpg.connect(
            host=DB_HOST,
            port=DB_PORT,
            user=DB_ADMIN,
            password=DB_PASSWORD,
            database="postgres"  # connect to default postgres DB
        )
        
        # Check if DB exists
        exists = await conn.fetchval(
            f"SELECT 1 FROM pg_database WHERE datname = '{DB_NAME}'"
        )
        
        if not exists:
            await conn.execute(f"CREATE DATABASE {DB_NAME}")
            print(f"✓ Database '{DB_NAME}' created")
        else:
            print(f"✓ Database '{DB_NAME}' already exists")
        
        await conn.close()
    except Exception as e:
        print(f"✗ Failed to create database: {e}")
        raise


async def create_tables():
    """Create all required tables in the database."""
    try:
        conn = await asyncpg.connect(
            host=DB_HOST,
            port=DB_PORT,
            user=DB_USER,
            password=DB_PASSWORD,
            database=DB_NAME
        )
        
        # ─────────────────────────────────────────────────────────
        # DRIVERS TABLE
        # ─────────────────────────────────────────────────────────
        await conn.execute("""
            CREATE TABLE IF NOT EXISTS drivers (
                driver_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                full_name VARCHAR(255) NOT NULL,
                email VARCHAR(255),
                phone VARCHAR(20),
                driver_type VARCHAR(50) DEFAULT 'regular',
                rating FLOAT DEFAULT 4.5,
                is_active BOOLEAN DEFAULT TRUE,
                is_online BOOLEAN DEFAULT FALSE,
                current_lat FLOAT,
                current_lng FLOAT,
                last_location_update TIMESTAMP,
                created_at TIMESTAMP DEFAULT NOW(),
                updated_at TIMESTAMP DEFAULT NOW()
            )
        """)
        print("✓ Created table: drivers")
        
        # ─────────────────────────────────────────────────────────
        # VEHICLES TABLE
        # ─────────────────────────────────────────────────────────
        await conn.execute("""
            CREATE TABLE IF NOT EXISTS vehicles (
                vehicle_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                driver_id UUID NOT NULL REFERENCES drivers(driver_id) ON DELETE CASCADE,
                vehicle_type VARCHAR(50) NOT NULL,
                license_plate VARCHAR(50),
                color VARCHAR(50),
                is_ac BOOLEAN DEFAULT TRUE,
                is_active BOOLEAN DEFAULT TRUE,
                created_at TIMESTAMP DEFAULT NOW(),
                UNIQUE(license_plate)
            )
        """)
        print("✓ Created table: vehicles")
        
        # ─────────────────────────────────────────────────────────
        # ETA_PREDICTIONS TABLE
        # ─────────────────────────────────────────────────────────
        await conn.execute("""
            CREATE TABLE IF NOT EXISTS eta_predictions (
                prediction_id SERIAL PRIMARY KEY,
                ride_id UUID,
                driver_id UUID,
                model_version VARCHAR(50),
                prediction_type VARCHAR(50),
                distance_km FLOAT,
                google_eta_sec INT,
                hour_of_day INT,
                day_of_week INT,
                is_weekend BOOLEAN,
                is_peak_hour BOOLEAN,
                traffic_score FLOAT,
                weather_condition VARCHAR(50),
                rainfall_mm FLOAT,
                google_eta_min FLOAT,
                ml_eta_min FLOAT,
                eta_correction_min FLOAT,
                confidence_score FLOAT,
                predicted_at TIMESTAMP DEFAULT NOW()
            )
        """)
        await conn.execute("""
            CREATE INDEX IF NOT EXISTS idx_eta_pred_ride ON eta_predictions(ride_id)
        """)
        await conn.execute("""
            CREATE INDEX IF NOT EXISTS idx_eta_pred_driver ON eta_predictions(driver_id)
        """)
        print("✓ Created table: eta_predictions")
        
        # ─────────────────────────────────────────────────────────
        # REROUTE_EVENTS TABLE
        # ─────────────────────────────────────────────────────────
        await conn.execute("""
            CREATE TABLE IF NOT EXISTS reroute_events (
                reroute_id SERIAL PRIMARY KEY,
                ride_id UUID,
                driver_id UUID,
                triggered_at TIMESTAMP DEFAULT NOW(),
                trigger_reason VARCHAR(255),
                driver_lat_at_reroute FLOAT,
                driver_lng_at_reroute FLOAT,
                old_eta_min FLOAT,
                new_eta_min FLOAT,
                old_distance_km FLOAT,
                new_distance_km FLOAT,
                time_saved_min FLOAT,
                google_api_called BOOLEAN DEFAULT TRUE,
                served_from_cache BOOLEAN DEFAULT FALSE,
                response_ms INT
            )
        """)
        await conn.execute("""
            CREATE INDEX IF NOT EXISTS idx_reroute_ride ON reroute_events(ride_id)
        """)
        await conn.execute("""
            CREATE INDEX IF NOT EXISTS idx_reroute_driver ON reroute_events(driver_id)
        """)
        print("✓ Created table: reroute_events")
        
        # ─────────────────────────────────────────────────────────
        # RIDES TABLE
        # ─────────────────────────────────────────────────────────
        await conn.execute("""
            CREATE TABLE IF NOT EXISTS rides (
                ride_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                driver_id UUID REFERENCES drivers(driver_id),
                pickup_zone_id INT,
                drop_zone_id INT,
                pickup_lat FLOAT,
                pickup_lng FLOAT,
                drop_lat FLOAT,
                drop_lng FLOAT,
                distance_km FLOAT,
                actual_duration_min FLOAT,
                ride_status VARCHAR(50) DEFAULT 'completed',
                traffic_congestion_score FLOAT,
                has_gps_anomaly BOOLEAN DEFAULT FALSE,
                hour_of_day INT,
                day_of_week INT,
                created_at TIMESTAMP DEFAULT NOW()
            )
        """)
        await conn.execute("""
            CREATE INDEX IF NOT EXISTS idx_rides_zone_pair 
            ON rides(pickup_zone_id, drop_zone_id, hour_of_day, day_of_week)
        """)
        print("✓ Created table: rides")
        
        # ─────────────────────────────────────────────────────────
        # TRAFFIC_PATTERNS TABLE
        # ─────────────────────────────────────────────────────────
        await conn.execute("""
            CREATE TABLE IF NOT EXISTS traffic_patterns (
                pattern_id SERIAL PRIMARY KEY,
                segment_id INT NOT NULL,
                hour_of_day INT,
                day_of_week INT,
                is_holiday BOOLEAN DEFAULT FALSE,
                avg_speed_kmph FLOAT,
                congestion_level VARCHAR(50),
                avg_delay_min_per_km FLOAT,
                vehicle_count INT,
                updated_at TIMESTAMP DEFAULT NOW()
            )
        """)
        await conn.execute("""
            CREATE INDEX IF NOT EXISTS idx_traffic_pattern 
            ON traffic_patterns(segment_id, hour_of_day, day_of_week, is_holiday)
        """)
        print("✓ Created table: traffic_patterns")
        
        # ─────────────────────────────────────────────────────────
        # ROUTE_CACHE TABLE
        # ─────────────────────────────────────────────────────────
        await conn.execute("""
            CREATE TABLE IF NOT EXISTS route_cache (
                cache_id SERIAL PRIMARY KEY,
                cache_key VARCHAR(255) UNIQUE,
                origin_lat FLOAT,
                origin_lng FLOAT,
                destination_lat FLOAT,
                destination_lng FLOAT,
                route_polyline TEXT,
                distance_km FLOAT,
                google_duration_sec INT,
                google_eta_traffic_sec INT,
                turn_by_turn_steps JSONB,
                cached_at TIMESTAMP DEFAULT NOW(),
                expires_at TIMESTAMP,
                hit_count INT DEFAULT 1
            )
        """)
        await conn.execute("""
            CREATE INDEX IF NOT EXISTS idx_route_cache_key ON route_cache(cache_key)
        """)
        print("✓ Created table: route_cache")
        
        # ─────────────────────────────────────────────────────────
        # MODEL_VERSIONS TABLE
        # ─────────────────────────────────────────────────────────
        await conn.execute("""
            CREATE TABLE IF NOT EXISTS model_versions (
                version_tag VARCHAR(50) PRIMARY KEY,
                algorithm VARCHAR(100),
                trained_at TIMESTAMP,
                is_active BOOLEAN DEFAULT FALSE,
                metrics JSONB,
                created_at TIMESTAMP DEFAULT NOW()
            )
        """)
        print("✓ Created table: model_versions")
        
        await conn.close()
        
    except Exception as e:
        print(f"✗ Failed to create tables: {e}")
        raise


async def check_postgresql_running():
    """Check if PostgreSQL is accessible."""
    try:
        conn = await asyncpg.connect(
            host=DB_HOST,
            port=DB_PORT,
            user=DB_ADMIN,
            password=DB_PASSWORD,
            database="postgres",
            timeout=5
        )
        await conn.close()
        return True
    except Exception as e:
        return False


async def main():
    """Main initialization flow."""
    print("=" * 60)
    print("Urban Black — Database Initialization")
    print("=" * 60)
    
    # Check if PostgreSQL is running
    print("\nChecking PostgreSQL connection...")
    if not await check_postgresql_running():
        print("\n✗ PostgreSQL is not running or not accessible!")
        print("\nPlease start PostgreSQL using one of these methods:")
        print("\n  Option 1 (Docker — Recommended):")
        print("    docker-compose up -d")
        print("\n  Option 2 (Local PostgreSQL):")
        print("    - Windows: Start 'PostgreSQL' service from Services.msc")
        print("    - Mac: brew services start postgresql")
        print("    - Linux: sudo systemctl start postgresql")
        print("\n  Option 3 (Connection Details):")
        print(f"    Host: {DB_HOST}:{DB_PORT}")
        print(f"    User: {DB_ADMIN}")
        print(f"    Password: {DB_PASSWORD}")
        print("\n" + "=" * 60)
        sys.exit(1)
    
    print("✓ PostgreSQL is accessible")
    
    try:
        print("\nStep 1: Creating database...")
        await create_database()
        
        print("\nStep 2: Creating tables...")
        await create_tables()
        
        print("\n" + "=" * 60)
        print("✓ Database initialization complete!")
        print("=" * 60)
        print(f"\nDatabase Details:")
        print(f"  Host: {DB_HOST}:{DB_PORT}")
        print(f"  Database: {DB_NAME}")
        print(f"  User: {DB_USER}")
        print(f"  Connection string:")
        print(f"    postgresql://{DB_USER}:{DB_PASSWORD}@{DB_HOST}:{DB_PORT}/{DB_NAME}")
        print(f"\nYour .env file is ready with these settings.")
        print(f"\nNext steps:")
        print(f"  1. Set GOOGLE_MAPS_API_KEY in .env (get from console.cloud.google.com)")
        print(f"  2. Run: python main.py")
        print(f"  3. Visit: http://localhost:8000/health")
        print("=" * 60)
        
    except Exception as e:
        print(f"\n✗ Initialization failed: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)


if __name__ == "__main__":
    import asyncio
    asyncio.run(main())
