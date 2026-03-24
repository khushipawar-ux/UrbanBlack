"""
services/cache.py — Redis caching layer for Google Maps API responses.

Cache key format: route:{o_lat3}_{o_lng3}_{d_lat3}_{d_lng3}
(coordinates rounded to 3 decimal places ≈ 111m precision)

TTL: 5 minutes (configurable via CACHE_TTL_SECONDS)
"""

import json
import hashlib
import structlog
from typing import Optional, Any
from config import settings

try:
    import redis.asyncio as aioredis
    REDIS_AVAILABLE = True
except ImportError:
    REDIS_AVAILABLE = False

log = structlog.get_logger(__name__)

# In-memory fallback when Redis is unavailable
_mem_cache: dict = {}


class CacheService:
    """
    Async Redis cache with in-memory fallback.
    All route/ETA responses are stored for CACHE_TTL_SECONDS (default 300s / 5 min).
    """

    def __init__(self):
        self._redis: Optional[Any] = None
        self._use_memory = False

    async def connect(self):
        """Connect to Redis. Falls back to in-memory cache on failure."""
        if not REDIS_AVAILABLE:
            log.warning("redis package not installed — using in-memory cache")
            self._use_memory = True
            return

        try:
            kwargs = {
                "host":     settings.REDIS_HOST,
                "port":     settings.REDIS_PORT,
                "db":       settings.REDIS_DB,
                "decode_responses": True,
                "socket_connect_timeout": 3,
                "socket_timeout": 3,
            }
            if settings.REDIS_PASSWORD:
                kwargs["password"] = settings.REDIS_PASSWORD

            self._redis = aioredis.Redis(**kwargs)
            await self._redis.ping()
            log.info("Redis connected",
                     host=settings.REDIS_HOST,
                     port=settings.REDIS_PORT)
        except Exception as e:
            log.warning("Redis unavailable — using in-memory cache", error=str(e))
            self._redis       = None
            self._use_memory  = True

    async def disconnect(self):
        if self._redis:
            await self._redis.close()
            log.info("Redis disconnected")

    # ─────────────────────────────────────────────────────────────────────
    # PUBLIC API
    # ─────────────────────────────────────────────────────────────────────

    async def get(self, key: str) -> Optional[dict]:
        """Return cached value or None."""
        try:
            if self._use_memory:
                return _mem_cache.get(key)
            raw = await self._redis.get(key)
            if raw:
                return json.loads(raw)
            return None
        except Exception as e:
            log.warning("Cache GET failed", key=key, error=str(e))
            return None

    async def set(self, key: str, value: dict, ttl: Optional[int] = None) -> bool:
        """Store value with TTL (defaults to settings.CACHE_TTL_SECONDS)."""
        ttl = ttl or settings.CACHE_TTL_SECONDS
        try:
            if self._use_memory:
                _mem_cache[key] = value
                return True
            await self._redis.setex(key, ttl, json.dumps(value))
            return True
        except Exception as e:
            log.warning("Cache SET failed", key=key, error=str(e))
            return False

    async def delete(self, key: str) -> bool:
        try:
            if self._use_memory:
                _mem_cache.pop(key, None)
                return True
            await self._redis.delete(key)
            return True
        except Exception as e:
            log.warning("Cache DELETE failed", key=key, error=str(e))
            return False

    async def is_healthy(self) -> bool:
        if self._use_memory:
            return True
        try:
            await self._redis.ping()
            return True
        except Exception:
            return False

    # ─────────────────────────────────────────────────────────────────────
    # ROUTE CACHE HELPERS
    # ─────────────────────────────────────────────────────────────────────

    @staticmethod
    def _r3(v: float) -> str:
        """
        Round a coordinate to 3 decimal places using string formatting.
        Avoids Python banker's rounding quirks — ensures nearby coordinates
        (within ~111m) always hash to the same cache key.
        """
        return f"{float(f'{v:.3f}')}"

    @staticmethod
    def make_route_key(
        origin_lat: float,
        origin_lng: float,
        dest_lat: float,
        dest_lng: float,
    ) -> str:
        """
        Build a cache key. Round to 3dp so nearby origins hit the same cache.
        3dp ≈ 111m precision — good enough for Pune city routes.
        """
        r = CacheService._r3
        return f"route:{r(origin_lat)}_{r(origin_lng)}_{r(dest_lat)}_{r(dest_lng)}"

    @staticmethod
    def make_eta_key(
        origin_lat: float,
        origin_lng: float,
        dest_lat: float,
        dest_lng: float,
        hour: int,
    ) -> str:
        """Separate cache for ETA requests (includes hour as part of key)."""
        r = CacheService._r3
        return f"eta:{r(origin_lat)}_{r(origin_lng)}_{r(dest_lat)}_{r(dest_lng)}_h{hour}"

    async def get_route(
        self,
        origin_lat: float, origin_lng: float,
        dest_lat: float,   dest_lng: float,
    ) -> Optional[dict]:
        key = self.make_route_key(origin_lat, origin_lng, dest_lat, dest_lng)
        cached = await self.get(key)
        if cached:
            log.info("Route cache HIT", key=key)
            return {**cached, "served_from_cache": True}
        return None

    async def set_route(
        self,
        origin_lat: float, origin_lng: float,
        dest_lat: float,   dest_lng: float,
        data: dict,
    ) -> bool:
        key = self.make_route_key(origin_lat, origin_lng, dest_lat, dest_lng)
        return await self.set(key, data)


# Global singleton
_cache: Optional[CacheService] = None


def get_cache() -> CacheService:
    global _cache
    if _cache is None:
        _cache = CacheService()
    return _cache
