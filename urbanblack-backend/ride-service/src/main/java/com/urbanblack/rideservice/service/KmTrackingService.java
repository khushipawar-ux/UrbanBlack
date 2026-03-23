package com.urbanblack.rideservice.service;

import com.urbanblack.rideservice.entity.DriverKmLog;
import com.urbanblack.rideservice.entity.KmCategory;
import com.urbanblack.rideservice.repository.DriverKmLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class KmTrackingService {

    /** Static goal km per shift (135 km) – for admin reporting. */
    public static final double GOAL_KM = 135.0;

    @Value("${ride.goal-km-per-shift:135}")
    private double goalKmPerShift;

    private final DriverKmLogRepository kmLogRepository;

    public void logKm(String driverId, String shiftId, KmCategory category, double km, String rideId) {
        DriverKmLog log = DriverKmLog.builder()
                .driverId(driverId)
                .shiftId(shiftId)
                .category(category)
                .km(km)
                .rideId(rideId)
                .recordedAt(LocalDateTime.now())
                .build();
        kmLogRepository.save(log);
    }

    /** Sums km by category for a given shift (shiftRef). */
    public double sumKmByShift(String shiftRef, KmCategory category) {
        Double sum = kmLogRepository.sumKmByCategoryAndShift(shiftRef, category);
        return sum != null ? sum : 0.0;
    }

    public double getGoalKmPerShift() {
        return goalKmPerShift;
    }
}

