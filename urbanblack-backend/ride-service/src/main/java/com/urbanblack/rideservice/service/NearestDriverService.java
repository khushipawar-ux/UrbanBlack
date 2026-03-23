package com.urbanblack.rideservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Finds nearby online/available drivers using Redis Geo index.
 *
 * <p>Each driver location update writes both:
 * <ul>
 *   <li>A plain JSON string at {@code driver:{id}:location} (for fast lookup)</li>
 *   <li>A geo-encoded point in the sorted set {@code drivers:geo} (for proximity search)</li>
 * </ul>
 *
 * <p>This service queries {@code drivers:geo} with GEOSEARCH and cross-checks
 * availability from the cached JSON to filter out offline/on-ride drivers.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NearestDriverService {

    /** Redis Geo sorted set key — all online drivers are members of this set. */
    public static final String GEO_KEY = "drivers:geo";

    /** Key pattern for cached driver location JSON — driver:{id}:location */
    private static final String LOCATION_KEY = "driver:%s:location";

    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    // ── Result model ─────────────────────────────────────────────────────────

    @Data
    @Builder
    public static class NearestDriverResult {
        /** Driver identifier. */
        private String driverId;
        /** Straight-line distance from pickup in kilometres (haversine). */
        private double distanceKm;
        /** Cached latitude of the driver. */
        private double lat;
        /** Cached longitude of the driver. */
        private double lng;
    }

    // ── Public API ───────────────────────────────────────────────────────────

    /**
     * Returns up to {@code limit} available drivers within {@code radiusKm} of
     * the given pickup coordinates, sorted by distance ascending.
     *
     * @param pickupLat   latitude of the pickup point
     * @param pickupLng   longitude of the pickup point
     * @param radiusKm    search radius in kilometres
     * @param limit       maximum number of results to return
     * @return list of nearest available drivers (may be empty, never null)
     */
    public List<NearestDriverResult> findNearestAvailableDrivers(double pickupLat,
                                                                  double pickupLng,
                                                                  double radiusKm,
                                                                  int limit) {
        GeoOperations<String, Object> geoOps = redisTemplate.opsForGeo();

        // Build GEOSEARCH args: within radius, sort ASC, include distance + coords
        RedisGeoCommands.GeoSearchCommandArgs args = RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs()
                .includeDistance()
                .includeCoordinates()
                .sortAscending()
                .limit(limit * 3); // fetch more to allow filtering by availability

        GeoResults<RedisGeoCommands.GeoLocation<Object>> results;
        try {
            results = geoOps.search(
                    GEO_KEY,
                    // Redis Geo uses (longitude, latitude) order
                    GeoReference.fromCoordinate(new Point(pickupLng, pickupLat)),
                    new Distance(radiusKm, Metrics.KILOMETERS),
                    args
            );
        } catch (Exception e) {
            log.warn("[NearestDriver] GEOSEARCH failed (Redis unavailable?): {}", e.getMessage());
            return List.of();
        }

        if (results == null || results.getContent().isEmpty()) {
            log.debug("[NearestDriver] No drivers in geo index within {}km of ({},{})",
                    radiusKm, pickupLat, pickupLng);
            return List.of();
        }

        List<NearestDriverResult> available = new ArrayList<>();

        for (var entry : results.getContent()) {
            if (available.size() >= limit) break;

            String driverId = String.valueOf(entry.getContent().getName());
            double distKm = entry.getDistance() != null
                    ? entry.getDistance().getValue() : 0.0;

            // Cross-check availability from cached JSON
            if (!isDriverAvailable(driverId)) {
                log.debug("[NearestDriver] Driver {} skipped — not available", driverId);
                continue;
            }

            // Extract lat/lng from WITHCOORD result
            double driverLat = 0.0, driverLng = 0.0;
            if (entry.getContent().getPoint() != null) {
                driverLng = entry.getContent().getPoint().getX(); // Redis returns lng first
                driverLat = entry.getContent().getPoint().getY();
            }

            available.add(NearestDriverResult.builder()
                    .driverId(driverId)
                    .distanceKm(Math.round(distKm * 100.0) / 100.0) // 2 decimal places
                    .lat(driverLat)
                    .lng(driverLng)
                    .build());
        }

        log.info("[NearestDriver] Found {} available driver(s) within {}km of ({},{})",
                available.size(), radiusKm, pickupLat, pickupLng);

        return available;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Checks whether a driver is currently available by reading their cached
     * JSON location payload from Redis.
     *
     * <p>The payload written by {@link DriverLocationService} includes an
     * {@code available} boolean field. A driver is considered available when:
     * <ul>
     *   <li>The key exists in Redis (driver has sent at least one location update)</li>
     *   <li>The {@code available} field is {@code true}</li>
     * </ul>
     */
    private boolean isDriverAvailable(String driverId) {
        String key = LOCATION_KEY.formatted(driverId);
        String json = stringRedisTemplate.opsForValue().get(key);
        if (json == null) {
            return false; // stale geo entry — no location cache
        }
        try {
            JsonNode node = objectMapper.readTree(json);
            // If the field is missing (old payload without availability), default to available
            if (!node.has("available")) return true;
            return node.get("available").asBoolean(false);
        } catch (Exception e) {
            log.warn("[NearestDriver] Could not parse availability for driver {}: {}", driverId, e.getMessage());
            return false;
        }
    }
}
