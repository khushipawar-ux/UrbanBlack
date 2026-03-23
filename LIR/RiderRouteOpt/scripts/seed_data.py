import asyncio
import os
import asyncpg
import logging

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

DATABASE_URL = os.getenv("DATABASE_URL", "postgresql://postgres:postgres@localhost:5432/lir")

async def seed_data():
    schema_path = os.path.join(os.path.dirname(__file__), "..", "..", "schema_pg17.sql")
    
    if not os.path.exists(schema_path):
        logger.error(f"Schema file not found at {schema_path}")
        return

    try:
        logger.info(f"Connecting to database at {DATABASE_URL}...")
        conn = await asyncpg.connect(DATABASE_URL)
        
        with open(schema_path, "r", encoding="utf-8") as f:
            sql = f.read()
            
        logger.info("Executing schema creations and dummy data insertions...")
        await conn.execute(sql)
        logger.info("Database seeded successfully with dummy data.")
        
        await conn.close()
    except Exception as e:
        logger.error(f"Failed to seed data: {e}")

if __name__ == "__main__":
    asyncio.run(seed_data())
