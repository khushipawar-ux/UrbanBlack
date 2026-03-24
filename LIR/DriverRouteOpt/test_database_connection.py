"""
test_database_connection.py — Test PostgreSQL database connectivity and basic operations

Usage:
    python test_database_connection.py

This script will:
1. Check if PostgreSQL is running
2. Verify credentials
3. Test basic queries
4. Show table status
5. Verify database schema
"""

import asyncio
import asyncpg
import sys
import os
from pathlib import Path
from dotenv import load_dotenv

# Load environment variables
load_dotenv()

# Database credentials from .env
DB_HOST = os.getenv("DB_HOST", "localhost")
DB_PORT = int(os.getenv("DB_PORT", 5432))
DB_USER = os.getenv("DB_USER", "postgres")
DB_PASSWORD = os.getenv("DB_PASSWORD", "postgres")
DB_NAME = os.getenv("DB_NAME", "urban_black_db")


async def test_database_connection():
    """Test database connection and basic operations."""
    
    print("=" * 70)
    print("Urban Black — Database Connection Test")
    print("=" * 70)
    
    # Test 1: Connection Parameters
    print("\n[1/6] Database Credentials")
    print(f"  Host:     {DB_HOST}:{DB_PORT}")
    print(f"  Database: {DB_NAME}")
    print(f"  User:     {DB_USER}")
    print(f"  Password: {'*' * len(DB_PASSWORD)}")
    
    # Test 2: Connect to PostgreSQL
    print("\n[2/6] Connecting to PostgreSQL...")
    try:
        conn = await asyncpg.connect(
            host=DB_HOST,
            port=DB_PORT,
            user=DB_USER,
            password=DB_PASSWORD,
            database=DB_NAME,
            timeout=5
        )
        print("  ✓ Connection successful!")
    except asyncpg.InvalidPasswordError:
        print("  ✗ Invalid password — check DB_PASSWORD in .env")
        return False
    except asyncpg.CannotConnectNowError:
        print("  ✗ PostgreSQL is not running")
        print("    → Start PostgreSQL service or Docker: docker-compose up -d")
        return False
    except asyncpg.ConnectionDoesNotExistError as e:
        print(f"  ✗ Database '{DB_NAME}' does not exist — run: python database/init_db.py")
        return False
    except Exception as e:
        print(f"  ✗ Connection failed: {e}")
        return False
    
    # Test 3: Ping the database
    print("\n[3/6] Ping Database")
    try:
        result = await conn.fetchval("SELECT 1")
        print(f"  ✓ Database responding with: {result}")
    except Exception as e:
        print(f"  ✗ Ping failed: {e}")
        await conn.close()
        return False
    
    # Test 4: Check tables exist
    print("\n[4/6] Checking Database Schema")
    try:
        tables = await conn.fetch("""
            SELECT table_name 
            FROM information_schema.tables 
            WHERE table_schema = 'public'
            ORDER BY table_name
        """)
        
        if tables:
            print(f"  ✓ Found {len(tables)} tables:")
            expected_tables = [
                'drivers', 'vehicles', 'eta_predictions', 'reroute_events',
                'rides', 'traffic_patterns', 'route_cache', 'model_versions'
            ]
            for table in tables:
                table_name = table['table_name']
                status = "✓" if table_name in expected_tables else "○"
                print(f"    {status} {table_name}")
        else:
            print("  ✗ No tables found — run: python database/init_db.py")
            await conn.close()
            return False
    except Exception as e:
        print(f"  ✗ Schema check failed: {e}")
        await conn.close()
        return False
    
    # Test 5: Check row counts
    print("\n[5/6] Table Row Counts")
    try:
        for table in ['drivers', 'eta_predictions', 'reroute_events', 'rides']:
            count = await conn.fetchval(f"SELECT COUNT(*) FROM {table}")
            print(f"  • {table:20} : {count:6} rows")
    except Exception as e:
        print(f"  ⚠ Could not get row counts: {e}")
    
    # Test 6: Test write operation (insert test row)
    print("\n[6/6] Testing Write Operation")
    try:
        # Try to insert a test driver
        test_driver_id = "12345678-1234-5678-1234-567812345678"
        
        # Check if already exists
        exists = await conn.fetchval(
            "SELECT 1 FROM drivers WHERE driver_id = $1::uuid LIMIT 1",
            test_driver_id
        )
        
        if not exists:
            await conn.execute(
                """INSERT INTO drivers (driver_id, full_name, email, phone, driver_type)
                   VALUES ($1::uuid, $2, $3, $4, $5)""",
                test_driver_id, "Test Driver", "test@urban-black.com", "9876543210", "regular"
            )
            print("  ✓ Successfully inserted test driver")
        else:
            print("  • Test driver already exists")
        
        # Query it back
        driver = await conn.fetchrow(
            "SELECT driver_id, full_name, email FROM drivers WHERE driver_id = $1::uuid",
            test_driver_id
        )
        if driver:
            print(f"  ✓ Read back: {driver['full_name']} ({driver['email']})")
    except Exception as e:
        print(f"  ✗ Write test failed: {e}")
        await conn.close()
        return False
    
    # Cleanup
    await conn.close()
    
    # Success!
    print("\n" + "=" * 70)
    print("✓ ALL TESTS PASSED — Database is working correctly!")
    print("=" * 70)
    print("""
Connection String:
  postgresql://{0}:{1}@{2}:{3}/{4}

You can now use the database with:
  • FastAPI app: python main.py
  • Direct queries: See database/queries.py for examples
  • Health check: http://localhost:8000/health (when server running)

Next Steps:
  1. Insert sample data into tables
  2. Run the FastAPI app: python main.py
  3. Test endpoints: http://localhost:8000/docs
  4. Check logs for database operations
""".format(DB_USER, "*" * len(DB_PASSWORD), DB_HOST, DB_PORT, DB_NAME))
    
    return True


async def show_connection_usage_examples():
    """Show examples of how to use database in Python."""
    print("\n" + "=" * 70)
    print("Database Usage Examples (from codebase)")
    print("=" * 70)
    
    print("""
1. **Simple Query (Single Row)**
   ─────────────────────────────
   async def get_driver(driver_id: str):
       pool = await get_pool()
       async with pool.acquire() as conn:
           driver = await conn.fetchrow(
               "SELECT * FROM drivers WHERE driver_id = $1::uuid",
               driver_id
           )
           return driver

2. **Insert with RETURNING**
   ────────────────────────
   async def create_prediction(ride_id, ml_eta_min):
       sql = "INSERT INTO eta_predictions (ride_id, ml_eta_min) 
              VALUES ($1::uuid, $2) RETURNING prediction_id"
       pool = await get_pool()
       async with pool.acquire() as conn:
           row = await conn.fetchrow(sql, ride_id, ml_eta_min)
           return row["prediction_id"]

3. **Update Operation**
   ──────────────────
   async def update_driver_location(driver_id, lat, lng):
       sql = "UPDATE drivers SET current_lat = $1, current_lng = $2 
              WHERE driver_id = $3::uuid"
       pool = await get_pool()
       async with pool.acquire() as conn:
           await conn.execute(sql, lat, lng, driver_id)

4. **Multiple Rows (fetch all)**
   ───────────────────────────
   async def get_nearby_drivers(lat, lng, radius_km):
       sql = "SELECT * FROM drivers WHERE 
              (6371 * acos(...)) < $1 ORDER BY distance ASC"
       pool = await get_pool()
       async with pool.acquire() as conn:
           rows = await conn.fetch(sql, radius_km, lat, lng)
           return [dict(row) for row in rows]

5. **Scalar Query (Single Value)**
   ──────────────────────────────
   count = await conn.fetchval("SELECT COUNT(*) FROM drivers")

6. **Transaction**
   ──────────────
   async with pool.acquire() as conn:
       async with conn.transaction():
           await conn.execute("INSERT INTO rides ...")
           await conn.execute("UPDATE drivers ...")

Key Patterns:
  • Always use parameters: $1, $2, $3 (never string formatting)
  • Use connection pool: await get_pool()
  • Always use context manager: async with pool.acquire() as conn:
  • Catch exceptions and log errors for resilience
  • Fire-and-forget for non-critical logging
""")


if __name__ == "__main__":
    try:
        success = asyncio.run(test_database_connection())
        if success:
            asyncio.run(show_connection_usage_examples())
        else:
            print("\n✗ Database tests failed — see errors above")
            sys.exit(1)
    except KeyboardInterrupt:
        print("\n\nTest cancelled")
        sys.exit(0)
