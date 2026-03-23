package com.urbanblack.rideservice.service;

import com.urbanblack.rideservice.entity.DriverShift;
import com.urbanblack.rideservice.entity.DriverStatus;
import com.urbanblack.rideservice.client.WalletServiceClient;
import com.urbanblack.rideservice.entity.KmCategory;
import com.urbanblack.rideservice.event.ShiftEndedEvent;
import com.urbanblack.rideservice.event.ShiftStartedEvent;
import com.urbanblack.rideservice.repository.DriverShiftRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Manages the ride-service view of a driver's active shift.
 * <p>
 * <b>Important:</b> This service does NOT expose REST shift endpoints.
 * All shift lifecycle transitions are driven by Kafka events published by driver-service:
 * <ul>
 *   <li>{@code driver.shift.started} → {@link #onShiftStarted(ShiftStartedEvent)}</li>
 *   <li>{@code driver.shift.ended}   → {@link #onShiftEnded(ShiftEndedEvent)}</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ShiftService {

    private final DriverShiftRepository shiftRepository;
    private final KmTrackingService kmTrackingService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final WalletServiceClient walletServiceClient;

    /**
     * Handles a {@code driver.shift.started} Kafka event from driver-service.
     * Creates a {@link DriverShift} record keyed by {@code shiftRef} for idempotency.
     */
    @Transactional
    public void onShiftStarted(ShiftStartedEvent event) {
        // Idempotency guard – ignore duplicate events
        if (shiftRepository.findByShiftRef(event.getShiftId()).isPresent()) {
            log.warn("[ShiftService] Duplicate driver.shift.started ignored | shiftRef={}",
                    event.getShiftId());
            return;
        }

        double goalKm = kmTrackingService.getGoalKmPerShift();

        DriverShift shift = DriverShift.builder()
                .driverId(event.getDriverId())
                .shiftRef(event.getShiftId())
                .shiftStart(event.getClockInTime() != null ? event.getClockInTime() : LocalDateTime.now())
                .status(DriverStatus.ON_SHIFT_AVAILABLE)
                .goalKm(goalKm)
                .goalKmReached(0d)
                .totalRideKm(0d)
                .totalDeadKm(0d)
                .totalFreeRoamingKm(0d)
                .totalKm(0d)
                .build();

        shiftRepository.save(shift);
        log.info("[ShiftService] DriverShift created | driverId={} shiftRef={}",
                event.getDriverId(), event.getShiftId());

        // Seed the geo index at clock-in location (if coordinates are from the event).
        if (event.getLatitude() != null && event.getLongitude() != null) {
            redisTemplate.opsForGeo().add(
                    NearestDriverService.GEO_KEY,
                    new Point(event.getLongitude(), event.getLatitude()),
                    event.getDriverId()
            );
            log.info("[ShiftService] Seeded geo index for driver {} at ({},{})",
                    event.getDriverId(), event.getLatitude(), event.getLongitude());
        }
    }

    /**
     * Handles a {@code driver.shift.ended} Kafka event from driver-service.
     * Syncs shift totals from km logs, then closes the {@link DriverShift} record.
     */
    @Transactional
    public void onShiftEnded(ShiftEndedEvent event) {
        DriverShift shift = shiftRepository.findByShiftRef(event.getShiftId())
                .orElseGet(() -> {
                    log.warn("[ShiftService] No DriverShift found for shiftRef={} – creating tombstone",
                            event.getShiftId());
                    double goalKm = kmTrackingService.getGoalKmPerShift();
                    return DriverShift.builder()
                            .driverId(event.getDriverId())
                            .shiftRef(event.getShiftId())
                            .shiftStart(event.getClockOutTime() != null
                                    ? event.getClockOutTime() : LocalDateTime.now())
                            .status(DriverStatus.OFF_DUTY)
                            .goalKm(goalKm)
                            .goalKmReached(0d)
                            .totalRideKm(0d)
                            .totalDeadKm(0d)
                            .totalFreeRoamingKm(0d)
                            .totalKm(0d)
                            .build();
                });

        // Sync totals from km logs before saving
        String shiftRef = shift.getShiftRef();
        double rideKm = kmTrackingService.sumKmByShift(shiftRef, KmCategory.RIDE_KM);
        double deadKm = kmTrackingService.sumKmByShift(shiftRef, KmCategory.DEAD_KM);
        double freeRoamingKm = kmTrackingService.sumKmByShift(shiftRef, KmCategory.FREE_ROAMING_KM);
        double totalKm = deadKm + freeRoamingKm + rideKm;

        shift.setTotalRideKm(rideKm);
        shift.setTotalDeadKm(deadKm);
        shift.setTotalFreeRoamingKm(freeRoamingKm);
        shift.setTotalKm(totalKm);
        shift.setGoalKmReached(freeRoamingKm);

        shift.setShiftEnd(event.getClockOutTime() != null ? event.getClockOutTime() : LocalDateTime.now());
        shift.setStatus(DriverStatus.OFF_DUTY);
        shiftRepository.save(shift);

        redisTemplate.opsForGeo().remove(NearestDriverService.GEO_KEY, event.getDriverId());
        log.info("[ShiftService] Shift closed | driverId={} shiftRef={} totalKm={} rideKm={} deadKm={} freeRoamingKm={} goalKmReached={}",
                event.getDriverId(), event.getShiftId(), totalKm, rideKm, deadKm, freeRoamingKm, freeRoamingKm);
    }
}
