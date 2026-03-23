package com.urbanblack.rideservice.controller;

import com.urbanblack.rideservice.dto.request.CompleteRideRequest;
import com.urbanblack.rideservice.dto.request.DriverLocationUpdateRequest;
import com.urbanblack.rideservice.dto.request.StartShiftRequest;
import com.urbanblack.rideservice.entity.Ride;
import com.urbanblack.rideservice.service.DriverLocationService;
import com.urbanblack.rideservice.service.RideService;
import com.urbanblack.rideservice.service.ShiftService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

import static java.lang.Math.*;

@RestController
@RequestMapping("/api/v1/driver")
@RequiredArgsConstructor
@Slf4j
public class DriverRideController {

    private String resolveDriverId(HttpServletRequest req) {
        String id = req.getHeader("X-User-Id");
        if (id == null || id.isBlank()) {
            id = req.getHeader("X-Driver-Id");
            if (id != null && !id.isBlank()) {
                log.debug("[DriverCtrl] X-User-Id missing — fell back to X-Driver-Id={}", id);
            }
        }
        return id;
    }

    private final ShiftService shiftService;
    private final RideService rideService;
    private final DriverLocationService driverLocationService;

    @PostMapping("/shift/start")
    public ResponseEntity<Map<String, Object>> startShift(@RequestBody StartShiftRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(Map.of(
                "error", "Shift start must come from driver-service Kafka event",
                "hint", "Use /drivers/api/shift/clock-in in driver-service"
        ));
    }

    @PostMapping("/shift/end")
    public ResponseEntity<Map<String, Object>> endShift() {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(Map.of(
                "error", "Shift end must come from driver-service Kafka event",
                "hint", "Use /drivers/api/shift/clock-out in driver-service"
        ));
    }

    @PostMapping("/location")
    public ResponseEntity<Map<String, Boolean>> updateLocation(
            @RequestBody DriverLocationUpdateRequest request,
            HttpServletRequest httpRequest) {
        String driverId = resolveDriverId(httpRequest);
        String activeRideId = rideService.getActiveRideForDriver(driverId)
                .map(Ride::getId)
                .orElse(null);

        driverLocationService.updateLocation(
                driverId,
                request.getLat(),
                request.getLng(),
                request.getBearing(),
                request.getSpeedKmh(),
                activeRideId,
                request.getVehicleType()
        );

        return ResponseEntity.ok(Map.of("ack", true));
    }

    @PostMapping("/rides/{rideId}/accept")
    public ResponseEntity<?> acceptRide(
            @PathVariable("rideId") String rideId,
            HttpServletRequest httpRequest) {
        String driverId = resolveDriverId(httpRequest);
        try {
            Ride ride = rideService.markDriverAccepted(rideId, driverId);
            return ResponseEntity.ok(ride);
        } catch (RideService.RideAlreadyAcceptedException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/rides/{rideId}/arrived")
    public ResponseEntity<Ride> markArrived(@PathVariable("rideId") String rideId) {
        Ride ride = rideService.markArrived(rideId);
        return ResponseEntity.ok(ride);
    }

    @PostMapping("/rides/{rideId}/start")
    public ResponseEntity<?> startRide(
            @PathVariable("rideId") String rideId,
            @RequestBody(required = false) Map<String, String> body) {
        String otp = body != null ? body.get("otp") : null;
        try {
            Ride ride = rideService.startRide(rideId, otp);
            return ResponseEntity.ok(ride);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/rides/{rideId}/complete")
    public ResponseEntity<?> completeRide(
            @PathVariable("rideId") String rideId,
            @RequestBody(required = false) CompleteRideRequest request) {
        try {
            Ride baseRide = rideService.getRide(rideId)
                    .orElseThrow(() -> new IllegalArgumentException("Ride not found"));

            double actualRideKm = baseRide.getRideKm() != null ? baseRide.getRideKm() : 0d;

            if (request != null
                    && request.getActualDropLat() != 0d
                    && request.getActualDropLng() != 0d
                    && baseRide.getPickupLat() != null
                    && baseRide.getPickupLng() != null) {
                actualRideKm = haversineKm(
                        baseRide.getPickupLat(),
                        baseRide.getPickupLng(),
                        request.getActualDropLat(),
                        request.getActualDropLng()
                );
            }

            Ride ride = rideService.completeRide(rideId, max(actualRideKm, 0.1d));
            return ResponseEntity.ok(ride);
        } catch (IllegalArgumentException e) {
            log.warn("[complete] Ride {}: {}", rideId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("[complete] Ride {} failed: ", rideId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to complete ride", "message", e.getMessage()));
        }
    }

    @GetMapping("/rides/active")
    public ResponseEntity<Ride> getActiveRide(HttpServletRequest httpRequest) {
        String driverId = resolveDriverId(httpRequest);
        return rideService.getActiveRideForDriver(driverId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @GetMapping("/rides/history")
    public ResponseEntity<?> getRideHistory(
            @RequestParam("page") int page,
            @RequestParam("size") int size,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            HttpServletRequest httpRequest) {
        String driverId = resolveDriverId(httpRequest);
        return ResponseEntity.ok(
                rideService.getDriverRideHistory(driverId, date, PageRequest.of(page, size))
        );
    }

    @GetMapping("/location")
    public ResponseEntity<Map<String, Object>> getLatestLocation(HttpServletRequest httpRequest) {
        String driverId = resolveDriverId(httpRequest);
        return ResponseEntity.ok(driverLocationService.getLatestLocation(driverId));
    }

    private double haversineKm(double lat1, double lng1, double lat2, double lng2) {
        double r = 6371d;
        double dLat = toRadians(lat2 - lat1);
        double dLng = toRadians(lng2 - lng1);
        double a = sin(dLat / 2) * sin(dLat / 2)
                + cos(toRadians(lat1)) * cos(toRadians(lat2))
                * sin(dLng / 2) * sin(dLng / 2);
        double c = 2 * atan2(sqrt(a), sqrt(1 - a));
        return r * c;
    }
}
