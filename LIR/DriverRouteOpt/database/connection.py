"""
database/connection.py — Async PostgreSQL connection pool (asyncpg + SQLAlchemy)
"""

import asyncpg
import structlog
from typing import Optional
from config import settings

log = structlog.get_logger(__name__)

# Global connection pool
_pool: Optional[asyncpg.Pool] = None


async def get_pool() -> asyncpg.Pool:
    """Return the global connection pool, creating it if needed."""
    global _pool
    if _pool is None:
        await create_pool()
    return _pool


async def create_pool():
    """Initialize the asyncpg connection pool."""
    global _pool
    try:
        _pool = await asyncpg.create_pool(
            host=settings.DB_HOST,
            port=settings.DB_PORT,
            database=settings.DB_NAME,
            user=settings.DB_USER,
            password=settings.DB_PASSWORD,
            min_size=settings.DB_POOL_MIN,
            max_size=settings.DB_POOL_MAX,
            command_timeout=30,
            statement_cache_size=0,   # safe for PgBouncer
        )
        log.info("Database pool created",
                 host=settings.DB_HOST,
                 db=settings.DB_NAME,
                 pool_min=settings.DB_POOL_MIN,
                 pool_max=settings.DB_POOL_MAX)
    except Exception as e:
        log.error("Failed to create database pool", error=str(e))
        raise


async def close_pool():
    """Gracefully close the connection pool on shutdown."""
    global _pool
    if _pool:
        await _pool.close()
        log.info("Database pool closed")
        _pool = None


async def check_connection() -> bool:
    """Ping the DB — used by /health endpoint."""
    try:
        pool = await get_pool()
        async with pool.acquire() as conn:
            await conn.fetchval("SELECT 1")
        return True
    except Exception as e:
        log.warning("DB health check failed", error=str(e))
        return False
