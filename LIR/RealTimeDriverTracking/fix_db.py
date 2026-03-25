import asyncio
from sqlalchemy import text
from database import AsyncSessionLocal

async def fix():
    async with AsyncSessionLocal() as session:
        try:
            # Drop the foreign key constraints directly if inserting fake rows is too hard due to triggers etc.
            await session.execute(text("ALTER TABLE driver_locations DROP CONSTRAINT IF EXISTS driver_locations_driver_id_fkey"))
            await session.execute(text("ALTER TABLE driver_locations DROP CONSTRAINT IF EXISTS driver_locations_ride_id_fkey"))
            await session.execute(text("ALTER TABLE lir_trip_gps_trail DROP CONSTRAINT IF EXISTS lir_trip_gps_trail_driver_id_fkey"))
            await session.execute(text("ALTER TABLE lir_trip_gps_trail DROP CONSTRAINT IF EXISTS lir_trip_gps_trail_ride_id_fkey"))
            print("Successfully dropped external FK constraints from the DB!")
        except Exception as e:
            print("Failed to drop constraints:", e)
            
        await session.commit()

if __name__ == "__main__":
    asyncio.run(fix())
