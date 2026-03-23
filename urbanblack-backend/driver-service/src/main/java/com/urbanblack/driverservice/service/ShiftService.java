package com.urbanblack.driverservice.service;

import com.urbanblack.common.dto.response.ApiResponse;
import com.urbanblack.driverservice.dto.ClockInRequest;
import com.urbanblack.driverservice.dto.ClockOutRequest;
import com.urbanblack.driverservice.entity.*;
import com.urbanblack.driverservice.event.ShiftEndedEvent;
import com.urbanblack.driverservice.event.ShiftStartedEvent;
import com.urbanblack.driverservice.kafka.ShiftEventProducer;
import com.urbanblack.driverservice.repository.ShiftRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShiftService {

    private final ShiftRepository shiftRepository;
    private final ShiftEventProducer shiftEventProducer;
    private final ShiftTimerManager timerManager;

    /** Default free-roaming KM quota per shift (configurable). */
    @Value("${ride.default-free-km-quota:12.0}")
    private double defaultFreeKmQuota;

    private static final long MAX_SHIFT_SECONDS = 43200;
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    @Transactional
    public ApiResponse<Shift> clockIn(String driverId, ClockInRequest req) {
        Optional<Shift> existing = shiftRepository.findByDriverIdAndStatus(driverId, ShiftStatus.ACTIVE);
        if (existing.isPresent()) {
            return ApiResponse.<Shift>builder()
                    .success(false)
                    .message("Driver already has an active shift")
                    .data(existing.get())
                    .build();
        }

        long nowEpoch = Instant.now().getEpochSecond();
        LocalDateTime nowIST = LocalDateTime.now(IST);

        Shift shift = Shift.builder()
                .driverId(driverId)
                .status(ShiftStatus.ACTIVE)
                .availability(DriverAvailability.ONLINE)
                .startingOdometer(req != null && req.getStartingOdometer() != null ? req.getStartingOdometer() : 0)
                .fuelLevelAtStart(req != null && req.getFuelLevel() != null ? req.getFuelLevel() : FuelLevel.HALF)
                .clockInTime(nowIST)
                .clockInLatitude(req != null ? req.getLatitude() : null)
                .clockInLongitude(req != null ? req.getLongitude() : null)
                .lastOnlineEpochSecond(nowEpoch)
                .lastOnlineTime(nowIST)
                .accumulatedActiveSeconds(0)
                .totalActiveMinutes(0)
                .build();

        shiftRepository.save(shift);
        log.info("CLOCK IN: driver={} | epoch={} | IST={}", driverId, nowEpoch, nowIST);

        // ── Publish structured shift-started event ──────────────────────
        shiftEventProducer.sendShiftStarted(ShiftStartedEvent.builder()
                .shiftId(shift.getId())
                .driverId(driverId)
                .latitude(shift.getClockInLatitude())
                .longitude(shift.getClockInLongitude())
                .clockInTime(nowIST)
                .freeKmQuota(defaultFreeKmQuota)
                .build());

        scheduleRemainingTime(shift);

        return ApiResponse.<Shift>builder()
                .success(true)
                .data(shift)
                .message("Clocked in successfully")
                .build();
    }

    @Transactional
    public ApiResponse<Shift> goOffline(String driverId) {
        log.info("GO OFFLINE request for driver: {}", driverId);
        Shift shift = getActiveShift(driverId);

        if (shift.getAvailability() == DriverAvailability.ONLINE) {
            long nowEpoch = Instant.now().getEpochSecond();
            LocalDateTime nowIST = LocalDateTime.now(IST);
            long startEpoch = resolveStartEpoch(shift, nowEpoch);

            long elapsed = Math.max(0, nowEpoch - startEpoch);
            long priorAccum = Math.max(0, shift.getAccumulatedActiveSeconds());
            long newAccum = priorAccum + elapsed;

            log.info("GO OFFLINE: Updating lastOnlineTime to event time: {}", nowIST);

            shift.setAccumulatedActiveSeconds(newAccum);
            shift.setAvailability(DriverAvailability.OFFLINE);

            // Record exact IST time when they went offline
            shift.setLastOnlineTime(nowIST);
            shift.setLastOfflineTime(nowIST);
            // lastOnlineEpochSecond is @Transient – no need to clear it

            timerManager.cancel(shift.getId());
            shiftRepository.save(shift);
        }

        return ApiResponse.<Shift>builder()
                .success(true)
                .data(shift)
                .message("Driver is offline")
                .build();
    }

    @Transactional
    public ApiResponse<Shift> goOnline(String driverId) {
        log.info("GO ONLINE request for driver: {}", driverId);
        Shift shift = getActiveShift(driverId);

        if (shift.getAvailability() == DriverAvailability.OFFLINE) {
            long nowEpoch = Instant.now().getEpochSecond();
            LocalDateTime nowIST = LocalDateTime.now(IST);

            shift.setAvailability(DriverAvailability.ONLINE);
            shift.setLastOnlineEpochSecond(nowEpoch); // transient – used for in-session calc
            shift.setLastOnlineTime(nowIST);
            shift.setLastOfflineTime(null);

            shiftRepository.save(shift);
            log.info("GO ONLINE: driver={} | epoch={} | IST={}", driverId, nowEpoch, nowIST);
            scheduleRemainingTime(shift);
        }

        return ApiResponse.<Shift>builder()
                .success(true)
                .data(shift)
                .message("Driver is online")
                .build();
    }

    @Transactional
    public ApiResponse<Shift> clockOut(String driverId, ClockOutRequest req) {
        Shift shift = getActiveShift(driverId);
        LocalDateTime clockOutTime = LocalDateTime.now(IST);

        updateOnlineDuration(shift);

        if (req != null) {
            if (req.getEndingOdometer() != null)
                shift.setEndingOdometer(req.getEndingOdometer());
            if (req.getFuelLevel() != null)
                shift.setFuelLevelAtEnd(req.getFuelLevel());
            if (req.getVehicleCondition() != null)
                shift.setVehicleCondition(req.getVehicleCondition());

            if (req.getLatitude() != null) {
                shift.setClockOutLatitude(req.getLatitude());
            }
            if (req.getLongitude() != null) {
                shift.setClockOutLongitude(req.getLongitude());
            }
        }

        completeShift(shift, clockOutTime);

        return ApiResponse.<Shift>builder()
                .success(true)
                .data(shift)
                .message("Shift completed. Total active: " + shift.getTotalActiveMinutes() + " mins")
                .build();
    }

    public ApiResponse<Shift> getShiftStatus(String driverId) {
        Optional<Shift> shiftOpt = shiftRepository.findByDriverIdAndStatus(driverId, ShiftStatus.ACTIVE);

        shiftOpt.ifPresent(s -> {
            if (s.getAvailability() == DriverAvailability.ONLINE
                    && s.getLastOnlineEpochSecond() == null) {
                long healEpoch = resolveStartEpoch(s, Instant.now().getEpochSecond());
                LocalDateTime healIST = Instant.ofEpochSecond(healEpoch).atZone(IST).toLocalDateTime();

                s.setLastOnlineEpochSecond(healEpoch);
                s.setLastOnlineTime(healIST);
                shiftRepository.save(s);

                log.info("HEALED shift {} -> epoch={} | IST={}", s.getId(), healEpoch, healIST);
            }
        });

        return ApiResponse.<Shift>builder()
                .success(true)
                .data(shiftOpt.orElse(null))
                .message(shiftOpt.isPresent() ? "Active shift found" : "No active shift")
                .build();
    }

    // ── Private helpers ────────────────────────────────────────────────────

    private long resolveStartEpoch(Shift shift, long nowEpoch) {
        if (shift.getLastOnlineEpochSecond() != null) {
            return shift.getLastOnlineEpochSecond();
        }
        if (shift.getClockInTime() != null) {
            long legacyEpoch = shift.getClockInTime()
                    .atZone(IST)
                    .toInstant()
                    .getEpochSecond();
            log.warn("lastOnlineEpochSecond null - using clockInTime epoch: {}", legacyEpoch);
            return legacyEpoch;
        }
        log.warn("Both lastOnlineEpochSecond and clockInTime are null - using nowEpoch");
        return nowEpoch;
    }

    private void updateOnlineDuration(Shift shift) {
        if (shift.getAvailability() == DriverAvailability.ONLINE) {
            long nowEpoch = Instant.now().getEpochSecond();
            long startEpoch = resolveStartEpoch(shift, nowEpoch);
            long elapsed = Math.max(0, nowEpoch - startEpoch);
            long priorAccum = Math.max(0, shift.getAccumulatedActiveSeconds());

            shift.setAccumulatedActiveSeconds(priorAccum + elapsed);

            // lastOnlineEpochSecond is @Transient; just keep display time updated
            shift.setLastOnlineTime(LocalDateTime.now(IST));

            log.info("updateOnlineDuration: elapsed={}s | total={}s", elapsed, (priorAccum + elapsed));
        }
    }

    private void scheduleRemainingTime(Shift shift) {
        long remaining = MAX_SHIFT_SECONDS - Math.max(0, shift.getAccumulatedActiveSeconds());
        if (remaining <= 0) {
            completeShift(shift, LocalDateTime.now(IST));
            return;
        }
        timerManager.schedule(shift.getId(), () -> autoComplete(shift.getDriverId()), remaining);
    }

    private void autoComplete(String driverId) {
        try {
            Shift shift = getActiveShift(driverId);
            updateOnlineDuration(shift);
            completeShift(shift, LocalDateTime.now(IST));
        } catch (RuntimeException ignored) {
        }
    }

    private void completeShift(Shift shift, LocalDateTime clockOutTimeIST) {
        shift.setStatus(ShiftStatus.COMPLETED);
        shift.setAvailability(DriverAvailability.OFFLINE);
        shift.setClockOutTime(clockOutTimeIST);

        // Set display time to clock-out time
        shift.setLastOnlineTime(clockOutTimeIST);
        shift.setLastOfflineTime(clockOutTimeIST);
        // lastOnlineEpochSecond is @Transient – not stored

        long accumulated = Math.max(0, shift.getAccumulatedActiveSeconds());
        shift.setTotalActiveMinutes(accumulated / 60);

        shiftRepository.save(shift);
        timerManager.cancel(shift.getId());

        log.info("SHIFT COMPLETED: driver={} | accumulated={}s -> {} mins",
                shift.getDriverId(), accumulated, shift.getTotalActiveMinutes());

        // ── Publish structured shift-ended event (replaces old raw-string send) ──
        shiftEventProducer.sendShiftEnded(ShiftEndedEvent.builder()
                .shiftId(shift.getId())
                .driverId(shift.getDriverId())
                .clockOutTime(clockOutTimeIST)
                .totalActiveMinutes(shift.getTotalActiveMinutes())
                .build());
    }

    private Shift getActiveShift(String driverId) {
        return shiftRepository
                .findByDriverIdAndStatus(driverId, ShiftStatus.ACTIVE)
                .orElseThrow(() -> new RuntimeException("No active shift found for driver: " + driverId));
    }
}