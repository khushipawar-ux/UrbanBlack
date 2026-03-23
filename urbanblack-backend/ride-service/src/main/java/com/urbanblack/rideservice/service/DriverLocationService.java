package com.urbanblack.rideservice.service;

import com.urbanblack.rideservice.entity.DriverLocation;
import com.urbanblack.rideservice.kafka.RideEventProducer;
import com.urbanblack.rideservice.repository.DriverLocationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Handles real-time driver location updates.
 *
 * <p>Every call to {@link #updateLocation} does three things atomically:
 * <ol>
 *   <li>Persists the location to PostgreSQL for history/analytics.</li>
 *   <li>Writes a JSON payload to {@code driver:{id}:location} in Redis for fast
 *       availability cross-checks by {@link NearestDriverService}.</li>
 *   <li>Updates the driver's position in the {@code drivers:geo} Redis Geo sorted
 *       set so that {@link NearestDriverService} can run O(log N) proximity
 *       searches without any external API call.</li>
 * </ol>
 *
 * <p>When a driver is on an active ride, they are removed from {@code drivers:geo}
 * so they are invisible to new ride-match searches.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DriverLocationService {

    private final DriverLocationRepository driverLocationRepository;
    private final StringRedisTemplate stringRedisTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final RideEventProducer eventProducer;

    /** Plain-JSON location cache: driver:{id}:location */
    private static final String DRIVER_LOCATION_KEY = "driver:%s:location";

    @Transactional
    public void updateLocation(String driverId,
                               Double lat,
                               Double lng,
                               Double bearing,
                               Double speedKmh,
                               String activeRideId,
                               String vehicleType) {

        // ── 1. Persist to PostgreSQL ─────────────────────────────────────────
        DriverLocation location = DriverLocation.builder()
                .driverId(driverId)
                .lat(lat)
                .lng(lng)
                .bearing(bearing)
                .speedKmh(speedKmh)
                .updatedAt(LocalDateTime.now())
                .build();
        driverLocationRepository.save(location);

        // ── 2. Cache latest location JSON (includes availability + vehicleType) ─
        // `available` = false when driver is already on a ride — NearestDriverService
        // reads this flag to filter out busy drivers without hitting the DB.
        // `vehicleType` = economy or premium — used to match ride requests by cab category.
        boolean available = (activeRideId == null);
        String key = DRIVER_LOCATION_KEY.formatted(driverId);
        String vt = (vehicleType != null && !vehicleType.isBlank())
                ? "\"" + vehicleType.trim().toLowerCase() + "\""
                : "null";
        String payload = String.format(
                "{\"lat\":%s,\"lng\":%s,\"bearing\":%s,\"speed_kmh\":%s,\"available\":%s,\"vehicleType\":%s}",
                lat, lng, bearing, speedKmh, available, vt);
        stringRedisTemplate.opsForValue().set(key, payload);

        // ── 3. Update Redis Geo index ─────────────────────────────────────────
        // Redis Geo expects (longitude, latitude) order — note X = lng, Y = lat.
        if (available) {
            // Driver is free — upsert their position in the geo pool
            redisTemplate.opsForGeo().add(
                    NearestDriverService.GEO_KEY,
                    new Point(lng, lat),   // lng first!
                    driverId
            );
            log.debug("[GeoIndex] Updated position for driver {} in {}", driverId, NearestDriverService.GEO_KEY);
        } else {
            // Driver is on a ride — remove from the pool so they are not matched
            redisTemplate.opsForGeo().remove(NearestDriverService.GEO_KEY, driverId);
            log.debug("[GeoIndex] Removed driver {} from {} (on active ride {})", driverId, NearestDriverService.GEO_KEY, activeRideId);
        }

        // ── 4. WebSocket broadcast to active ride passengers ─────────────────
        if (activeRideId != null) {
            messagingTemplate.convertAndSend(
                    "/topic/ride/" + activeRideId + "/driver-location",
                    Map.of(
                            "lat", lat,
                            "lng", lng,
                            "bearing", bearing,
                            "speed_kmh", speedKmh
                    )
            );
        }

        // ── 5. Kafka event for analytics / audit ─────────────────────────────
        eventProducer.sendDriverLocationUpdated(location, driverId);
    }

    /**
     * Retrieves the latest cached location for a driver from Redis.
     */
    public Map<String, Object> getLatestLocation(String driverId) {
        String key = DRIVER_LOCATION_KEY.formatted(driverId);
        String json = stringRedisTemplate.opsForValue().get(key);
        
        if (json == null) {
            // Fallback to DB if Redis is empty
            return driverLocationRepository.findFirstByDriverIdOrderByUpdatedAtDesc(driverId)
                    .map(loc -> Map.<String, Object>of(
                            "lat", loc.getLat(),
                            "lng", loc.getLng(),
                            "bearing", loc.getBearing() != null ? loc.getBearing() : 0.0,
                            "speed_kmh", loc.getSpeedKmh() != null ? loc.getSpeedKmh() : 0.0,
                            "available", true
                    ))
                    .orElse(Map.of());
        }

        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(json, Map.class);
        } catch (Exception e) {
            log.error("Failed to parse location JSON from Redis: {}", json, e);
            return Map.of();
        }
    }
}

