"""
database/queries.py — All database read/write operations
"""

import uuid
import structlog
from datetime import datetime
from typing import Optional, Dict, Any, List
from database.connection import get_pool

log = structlog.get_logger(__name__)


# ─────────────────────────────────────────────────────────────────────────────
# RIDE QUERIES
# ─────────────────────────────────────────────────────────────────────────────

async def log_eta_prediction(
    ride_id: Optional[str],
    driver_id: Optional[str],
    model_version: str,
    prediction_type: str,
    distance_km: float,
    google_eta_sec: int,
    hour_of_day: int,
    day_of_week: int,
    is_weekend: bool,
    is_peak_hour: bool,
    traffic_score: float,
    weather_condition: str,
    rainfall_mm: float,
    google_eta_min: float,
    ml_eta_min: float,
    eta_correction_min: float,
    confidence_score: Optional[float] = None,
) -> Optional[int]:
    """Insert a row into eta_predictions for every ML inference."""
    sql = """
        INSERT INTO eta_predictions (
            ride_id, driver_id, model_version, prediction_type,
            distance_km, google_eta_sec, hour_of_day, day_of_week,
            is_weekend, is_peak_hour, traffic_score, weather_condition,
            rainfall_mm, google_eta_min, ml_eta_min, eta_correction_min,
            confidence_score, predicted_at
        ) VALUES (
            $1::uuid, $2::uuid, $3, $4,
            $5, $6, $7, $8,
            $9, $10, $11, $12,
            $13, $14, $15, $16,
            $17, NOW()
        )
        RETURNING prediction_id
    """
    try:
        pool = await get_pool()
        async with pool.acquire() as conn:
            row = await conn.fetchrow(
                sql,
                ride_id, driver_id, model_version, prediction_type,
                distance_km, google_eta_sec, hour_of_day, day_of_week,
                is_weekend, is_peak_hour, traffic_score, weather_condition,
                rainfall_mm, google_eta_min, ml_eta_min, eta_correction_min,
                confidence_score,
            )
            return row["prediction_id"] if row else None
    except Exception as e:
        log.error("Failed to log ETA prediction", error=str(e))
        return None


async def log_reroute_event(
    ride_id: str,
    driver_id: str,
    trigger_reason: str,
    driver_lat: float,
    driver_lng: float,
    old_eta_min: float,
    new_eta_min: float,
    old_distance_km: float,
    new_distance_km: float,
    response_ms: int,
    served_from_cache: bool = False,
) -> Optional[int]:
    """Log every reroute event."""
    sql = """
        INSERT INTO reroute_events (
            ride_id, driver_id, triggered_at, trigger_reason,
            driver_lat_at_reroute, driver_lng_at_reroute,
            old_eta_min, new_eta_min, old_distance_km, new_distance_km,
            time_saved_min, google_api_called, served_from_cache, response_ms
        ) VALUES (
            $1::uuid, $2::uuid, NOW(), $3,
            $4, $5,
            $6, $7, $8, $9,
            $10, $11, $12, $13
        )
        RETURNING reroute_id
    """
    time_saved = round(old_eta_min - new_eta_min, 2)
    try:
        pool = await get_pool()
        async with pool.acquire() as conn:
            row = await conn.fetchrow(
                sql,
                ride_id, driver_id, trigger_reason,
                driver_lat, driver_lng,
                old_eta_min, new_eta_min, old_distance_km, new_distance_km,
                time_saved, not served_from_cache, served_from_cache, response_ms,
            )
            return row["reroute_id"] if row else None
    except Exception as e:
        log.error("Failed to log reroute event", error=str(e))
        return None


async def update_driver_location(
    driver_id: str,
    latitude: float,
    longitude: float,
) -> bool:
    """Update driver's current GPS position."""
    sql = """
        UPDATE drivers
        SET current_lat = $1, current_lng = $2, last_location_update = NOW()
        WHERE driver_id = $3::uuid
    """
    try:
        pool = await get_pool()
        async with pool.acquire() as conn:
            await conn.execute(sql, latitude, longitude, driver_id)
        return True
    except Exception as e:
        log.warning("Failed to update driver location", driver_id=driver_id, error=str(e))
        return False


async def get_traffic_pattern(
    segment_id: int,
    hour_of_day: int,
    day_of_week: int,
    is_holiday: bool = False,
) -> Optional[Dict[str, Any]]:
    """
    Fetch historical traffic pattern for a road segment at a given time.
    Used to cross-check if Google's route is typically good at this hour.
    """
    sql = """
        SELECT avg_speed_kmph, congestion_level, avg_delay_min_per_km, vehicle_count
        FROM traffic_patterns
        WHERE segment_id = $1
          AND hour_of_day = $2
          AND day_of_week = $3
          AND is_holiday  = $4
        LIMIT 1
    """
    try:
        pool = await get_pool()
        async with pool.acquire() as conn:
            row = await conn.fetchrow(sql, segment_id, hour_of_day, day_of_week, is_holiday)
            return dict(row) if row else None
    except Exception as e:
        log.warning("Failed to fetch traffic pattern", error=str(e))
        return None


async def get_nearby_drivers(
    lat: float,
    lng: float,
    radius_km: float = 3.0,
    limit: int = 5,
) -> List[Dict[str, Any]]:
    """
    Find online drivers within radius_km of a location.
    Uses the Haversine formula in SQL.
    """
    sql = """
        SELECT
            d.driver_id,
            d.full_name,
            d.driver_type,
            d.rating,
            d.current_lat,
            d.current_lng,
            v.vehicle_type,
            v.is_ac,
            (
                6371 * acos(
                    cos(radians($1)) * cos(radians(d.current_lat)) *
                    cos(radians(d.current_lng) - radians($2)) +
                    sin(radians($1)) * sin(radians(d.current_lat))
                )
            ) AS distance_km
        FROM drivers d
        JOIN vehicles v ON v.driver_id = d.driver_id AND v.is_active = TRUE
        WHERE d.is_online = TRUE
          AND d.is_active = TRUE
          AND d.current_lat IS NOT NULL
        HAVING distance_km < $3
        ORDER BY distance_km ASC
        LIMIT $4
    """
    try:
        pool = await get_pool()
        async with pool.acquire() as conn:
            rows = await conn.fetch(sql, lat, lng, radius_km, limit)
            return [dict(r) for r in rows]
    except Exception as e:
        log.warning("Failed to fetch nearby drivers", error=str(e))
        return []


async def get_historical_route_avg(
    pickup_zone_id: int,
    drop_zone_id: int,
    hour_of_day: int,
    day_of_week: int,
) -> Optional[Dict[str, Any]]:
    """
    Returns avg actual_duration_min for historical rides on similar zone pair
    at a similar time. Used as extra signal for ML correction.
    """
    sql = """
        SELECT
            COUNT(*)                        AS sample_count,
            AVG(actual_duration_min)        AS avg_actual_min,
            AVG(distance_km)                AS avg_distance_km,
            AVG(traffic_congestion_score)   AS avg_traffic_score
        FROM rides
        WHERE pickup_zone_id = $1
          AND drop_zone_id   = $2
          AND ABS(hour_of_day - $3) <= 1
          AND day_of_week = $4
          AND ride_status = 'completed'
          AND has_gps_anomaly = FALSE
    """
    try:
        pool = await get_pool()
        async with pool.acquire() as conn:
            row = await conn.fetchrow(sql, pickup_zone_id, drop_zone_id, hour_of_day, day_of_week)
            return dict(row) if row and row["sample_count"] > 0 else None
    except Exception as e:
        log.warning("Failed to fetch historical route avg", error=str(e))
        return None


async def save_route_cache_to_db(
    cache_key: str,
    origin_lat: float, origin_lng: float,
    dest_lat: float, dest_lng: float,
    polyline: str,
    distance_km: float,
    google_duration_sec: int,
    google_eta_traffic_sec: int,
    steps_json: str,
    ttl_seconds: int = 300,
) -> bool:
    """Mirror Redis cache into DB for audit and fallback."""
    sql = """
        INSERT INTO route_cache (
            cache_key, origin_lat, origin_lng, destination_lat, destination_lng,
            route_polyline, distance_km, google_duration_sec, google_eta_traffic_sec,
            turn_by_turn_steps, cached_at, expires_at, hit_count
        ) VALUES (
            $1, $2, $3, $4, $5,
            $6, $7, $8, $9,
            $10, NOW(), NOW() + ($11 || ' seconds')::interval, 1
        )
        ON CONFLICT (cache_key) DO UPDATE
        SET hit_count   = route_cache.hit_count + 1,
            cached_at   = NOW(),
            expires_at  = NOW() + ($11 || ' seconds')::interval
    """
    try:
        pool = await get_pool()
        async with pool.acquire() as conn:
            await conn.execute(
                sql,
                cache_key, origin_lat, origin_lng, dest_lat, dest_lng,
                polyline, distance_km, google_duration_sec, google_eta_traffic_sec,
                steps_json, str(ttl_seconds),
            )
        return True
    except Exception as e:
        log.warning("Failed to save route cache to DB", error=str(e))
        return False


async def get_active_model_version() -> Optional[str]:
    """Get the currently active model version tag."""
    sql = "SELECT version_tag FROM model_versions WHERE is_active = TRUE LIMIT 1"
    try:
        pool = await get_pool()
        async with pool.acquire() as conn:
            row = await conn.fetchrow(sql)
            return row["version_tag"] if row else None
    except Exception as e:
        log.warning("Failed to fetch active model version", error=str(e))
        return None
