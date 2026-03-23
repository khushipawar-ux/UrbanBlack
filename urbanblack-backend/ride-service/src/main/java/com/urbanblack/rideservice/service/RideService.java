package com.urbanblack.rideservice.service;

import com.urbanblack.rideservice.client.DriverServiceClient;
import com.urbanblack.rideservice.client.UserServiceClient;
import com.urbanblack.rideservice.client.WalletServiceClient;
import com.urbanblack.rideservice.dto.DriverSummaryDto;
import com.urbanblack.rideservice.entity.Ride;
import com.urbanblack.rideservice.entity.RideStatus;
import com.urbanblack.rideservice.kafka.RideEventProducer;
import com.urbanblack.rideservice.repository.RideRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class RideService {

    private final RideRepository rideRepository;
    private final FareService fareService;
    private final GeoService geoService;
    private final RideEventProducer eventProducer;
    private final NearestDriverService nearestDriverService;
    private final RideNotificationService notificationService;

    private final RewardTreeService rewardTreeService;
    private final UserServiceClient userServiceClient;
    private final WalletServiceClient walletServiceClient;
    private final DriverServiceClient driverServiceClient;


    /**
     * Creates a minimal completed ride for reward tree testing.
     * Used by admin/test endpoint POST /api/v1/rides/complete.
     */
    @Transactional
    public Ride createRideForReward(String userId, BigDecimal fare) {
        Ride ride = Ride.builder()
                .userId(userId)
                .pickupLat(0.0)
                .pickupLng(0.0)
                .dropLat(0.0)
                .dropLng(0.0)
                .status(RideStatus.RIDE_COMPLETED)
                .rideKm(0.0)
                .fare(fare != null ? fare : BigDecimal.ZERO)
                .requestedAt(LocalDateTime.now())
                .completedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return rideRepository.save(ride);
    }

    @Transactional
    public Ride estimateRide(double pickupLat,
                             double pickupLng,
                             double dropLat,
                             double dropLng) {
        GeoService.RouteEstimate estimate = geoService.estimateRideRoute(pickupLat, pickupLng, dropLat, dropLng);
        BigDecimal fare = fareService.calculateFare(estimate.getDistanceKm());

        return Ride.builder()
                .pickupLat(pickupLat)
                .pickupLng(pickupLng)
                .dropLat(dropLat)
                .dropLng(dropLng)
                .rideKm(estimate.getDistanceKm())
                .durationMin(estimate.getDurationMin())
                .fare(fare)
                .status(RideStatus.REQUESTED)
                .requestedAt(LocalDateTime.now())
                .build();
    }

    @Transactional
    public Ride createRideRequest(String userId,
                                  double pickupLat,
                                  double pickupLng,
                                  double dropLat,
                                  double dropLng,
                                  String pickupAddress,
                                  String dropAddress,
                                  String notes,
                                  String vehicleType) {
        GeoService.RouteEstimate estimate = geoService.estimateRideRoute(pickupLat, pickupLng, dropLat, dropLng);
        BigDecimal fare = fareService.calculateFare(estimate.getDistanceKm());

        // Normalize vehicleType for matching (economy/premium)
        String normalizedVehicleType = (vehicleType != null && !vehicleType.isBlank())
                ? vehicleType.trim().toLowerCase()
                : null;

        // Find nearby available drivers using Redis GeoSearch (no cab-category filtering)
        List<NearestDriverService.NearestDriverResult> nearbyDrivers =
                nearestDriverService.findNearestAvailableDrivers(pickupLat, pickupLng, 5.0, 5);

        log.info("[RideService] {} nearby driver(s) found for ride request by userId={}",
                nearbyDrivers.size(), userId);

        Ride ride = Ride.builder()
                .userId(userId)
                .pickupLat(pickupLat)
                .pickupLng(pickupLng)
                .dropLat(dropLat)
                .dropLng(dropLng)
                .pickupAddress(pickupAddress)
                .dropAddress(dropAddress)
                .notes(notes)
                .vehicleType(normalizedVehicleType)
                .rideKm(estimate.getDistanceKm())
                .durationMin(estimate.getDurationMin())
                .fare(fare)
                .status(nearbyDrivers.isEmpty() ? RideStatus.REQUESTED : RideStatus.DRIVERS_NOTIFIED)
                .requestedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        Ride saved = rideRepository.save(ride);

        // Attach nearby driver info for the response layer (transient — not persisted)
        saved.setNearbyDrivers(nearbyDrivers);

        eventProducer.sendRideRequested(saved);

        // Resolve passenger name + phone from user-service
        UserInfo userInfo = resolveUserInfo(userId);
        saved.setUserName(userInfo.name);
        saved.setUserPhone(userInfo.phone);

        String scheduledTime = saved.getRequestedAt() != null
                ? saved.getRequestedAt().toString()
                : "Now";

        notificationService.notifyNearbyDrivers(
                saved.getId(),
                pickupLat, pickupLng, pickupAddress,
                dropLat, dropLng, dropAddress,
                saved.getRideKm() != null ? saved.getRideKm() : 0,
                saved.getFare() != null ? saved.getFare().doubleValue() : 0,
                userInfo.name,
                userInfo.rating,
                scheduledTime,
                nearbyDrivers
        );

        return saved;
    }

    public Optional<Ride> getRide(String rideId) {
        return rideRepository.findById(rideId);
    }

    public Optional<Ride> getActiveRideForUser(String userId) {
        List<RideStatus> activeStatuses = List.of(
                RideStatus.DRIVERS_NOTIFIED,
                RideStatus.DRIVER_ACCEPTED,
                RideStatus.DRIVER_EN_ROUTE,
                RideStatus.DRIVER_ARRIVED,
                RideStatus.RIDE_STARTED
        );
        return rideRepository.findFirstByUserIdAndStatusInOrderByCreatedAtDesc(userId, activeStatuses);
    }

    public Optional<Ride> getActiveRideForDriver(String driverId) {
        String effectiveDriverId = resolveToEmployeeId(driverId);
        List<RideStatus> activeStatuses = List.of(
                RideStatus.DRIVER_ACCEPTED,
                RideStatus.DRIVER_EN_ROUTE,
                RideStatus.DRIVER_ARRIVED,
                RideStatus.RIDE_STARTED
        );
        return rideRepository.findFirstByDriverIdAndStatusInOrderByCreatedAtDesc(effectiveDriverId, activeStatuses);
    }

    public Page<Ride> getUserRideHistory(String userId, Pageable pageable) {
        return rideRepository.findByUserIdOrderByRequestedAtDesc(userId, pageable);
    }

    public Page<Ride> getDriverRideHistory(String driverId, LocalDate date, Pageable pageable) {
        String effectiveDriverId = resolveToEmployeeId(driverId);
        LocalDateTime from = date.atStartOfDay();
        LocalDateTime to = date.plusDays(1).atStartOfDay().minusNanos(1);
        return rideRepository.findByDriverIdAndRequestedAtBetweenOrderByRequestedAtDesc(effectiveDriverId, from, to, pageable);
    }

    /**
     * Small value object for resolved user information.
     */
    private static class UserInfo {
        final String name;
        final String phone;
        final Double rating;

        UserInfo(String name, String phone, Double rating) {
            this.name = name;
            this.phone = phone;
            this.rating = rating;
        }
    }

    /**
     * Resolves user name / phone (and optionally rating) from user-service,
     * falling back to userId when lookup fails.
     */
    private UserInfo resolveUserInfo(String userId) {
        String name = userId;
        String phone = null;
        Double rating = null;
        try {
            Map<String, Object> userResponse = userServiceClient.getUserById(userId);
            if (userResponse != null) {
                Object data = userResponse.get("data");
                if (data instanceof Map) {
                    Map<?, ?> userData = (Map<?, ?>) data;
                    Object nameObj = userData.get("name");
                    Object phoneObj = userData.get("phone");
                    if (nameObj != null) name = nameObj.toString();
                    if (phoneObj != null) phone = phoneObj.toString();
                }
            }
        } catch (Exception e) {
            log.warn("[RideService] Could not fetch user info for userId={}: {}", userId, e.getMessage());
        }
        return new UserInfo(name, phone, rating);
    }

    @Transactional
    public Ride cancelRide(String rideId, String reason) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new IllegalArgumentException("Ride not found"));
        ride.setStatus(RideStatus.CANCELLED);
        ride.setUpdatedAt(LocalDateTime.now());
        Ride saved = rideRepository.save(ride);
        eventProducer.sendRideCancelled(saved);

        // Notify both rider and driver via WebSocket
        notificationService.notifyRideStatusChange(
                rideId, "CANCELLED", saved.getUserId(), saved.getDriverId());

        return saved;
    }

    /**
     * Accepts a ride for a specific driver. Uses optimistic locking (@Version) to prevent
     * two drivers from accepting the same ride simultaneously.
     *
     * @throws IllegalStateException if the ride is not in DRIVERS_NOTIFIED status
     * @throws RideAlreadyAcceptedException if another driver already accepted (concurrent update)
     */
    @Transactional
    public Ride markDriverAccepted(String rideId, String driverId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new IllegalArgumentException("Ride not found"));

        // Only rides in DRIVERS_NOTIFIED status can be accepted
        if (ride.getStatus() != RideStatus.DRIVERS_NOTIFIED && ride.getStatus() != RideStatus.REQUESTED) {
            throw new RideAlreadyAcceptedException(
                    "Ride " + rideId + " is already in status " + ride.getStatus() + " and cannot be accepted");
        }

        // Use employeeId as driverId for ride/payment flow (admin assigns cab/depot/shift by empId)
        String effectiveDriverId = resolveToEmployeeId(driverId);

        ride.setDriverId(effectiveDriverId);
        ride.setStatus(RideStatus.DRIVER_ACCEPTED);
        ride.setUpdatedAt(LocalDateTime.now());

        // Generate a 4-digit OTP for trip verification
        String otp = String.format("%04d", new Random().nextInt(10000));
        ride.setOtp(otp);

        try {
            Ride saved = rideRepository.save(ride);
            eventProducer.sendRideAccepted(saved);

            // Resolve user info so the driver sees the passenger's real name
            UserInfo userInfo = resolveUserInfo(saved.getUserId());
            saved.setUserName(userInfo.name);
            saved.setUserPhone(userInfo.phone);

            // Notify rider: "Driver is on the way" (include OTP so user can display it instantly)
            notificationService.notifyRideStatusChange(
                    rideId, "DRIVER_ACCEPTED", saved.getUserId(), effectiveDriverId, otp);

            return saved;
        } catch (OptimisticLockingFailureException e) {
            log.warn("[RideService] Concurrent accept for ride {} by driver {} — another driver won", rideId, effectiveDriverId);
            throw new RideAlreadyAcceptedException("Ride " + rideId + " was accepted by another driver");
        }
    }

    /**
     * Resolves driverId (UUID or employeeId) to employeeId for ride/payment flow.
     * Admin assigns cab/depot/shift by employeeId; payment analytics use employeeId.
     */
    private String resolveToEmployeeId(String driverId) {
        if (driverId == null || driverId.isBlank()) return driverId;
        try {
            DriverSummaryDto summary = driverServiceClient.getDriverSummary(driverId);
            if (summary != null && summary.getEmployeeId() != null && !summary.getEmployeeId().isBlank()) {
                return summary.getEmployeeId();
            }
        } catch (Exception e) {
            log.debug("[RideService] Could not resolve employeeId for driver {}: {}", driverId, e.getMessage());
        }
        return driverId;
    }

    /** Thrown when a driver tries to accept a ride that's already been taken. */
    public static class RideAlreadyAcceptedException extends RuntimeException {
        public RideAlreadyAcceptedException(String message) {
            super(message);
        }
    }

    @Transactional
    public Ride markArrived(String rideId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new IllegalArgumentException("Ride not found"));
        ride.setStatus(RideStatus.DRIVER_ARRIVED);
        ride.setUpdatedAt(LocalDateTime.now());
        Ride saved = rideRepository.save(ride);

        notificationService.notifyRideStatusChange(
                rideId, "DRIVER_ARRIVED", saved.getUserId(), saved.getDriverId());

        return saved;
    }

    @Transactional
    public Ride startRide(String rideId) {
        return startRide(rideId, null);
    }

    @Transactional
    public Ride startRide(String rideId, String enteredOtp) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new IllegalArgumentException("Ride not found"));

        if (enteredOtp != null && !enteredOtp.isBlank()) {
            String storedOtp = ride.getOtp();
            if (storedOtp != null && !storedOtp.isBlank() && !storedOtp.equals(enteredOtp.trim())) {
                throw new IllegalArgumentException("Invalid OTP. Please ask the passenger for the correct code.");
            }
        }

        ride.setStatus(RideStatus.RIDE_STARTED);
        ride.setStartedAt(LocalDateTime.now());
        ride.setUpdatedAt(LocalDateTime.now());
        Ride saved = rideRepository.save(ride);

        notificationService.notifyRideStatusChange(
                rideId, "RIDE_STARTED", saved.getUserId(), saved.getDriverId());

        return saved;
    }

    @Transactional
    public Ride completeRide(String rideId, double actualRideKm) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new IllegalArgumentException("Ride not found"));
        if (ride.getStatus() == RideStatus.RIDE_COMPLETED) {
            log.debug("Ride {} already completed, returning as-is", rideId);
            return ride;
        }
        ride.setRideKm(actualRideKm);
        BigDecimal fare = fareService.calculateFare(actualRideKm);
        ride.setFare(fare);
        ride.setStatus(RideStatus.RIDE_COMPLETED);
        ride.setCompletedAt(LocalDateTime.now());
        ride.setUpdatedAt(LocalDateTime.now());
        ride.setPaymentStatus(Ride.PaymentStatus.PENDING);
        ride.setWalletUsed(BigDecimal.ZERO);
        ride.setOnlinePaid(BigDecimal.ZERO);
        Ride saved = rideRepository.save(ride);

        // Payment via PayU UPI only - Kafka event triggers payment-service to create payment record
        // User pays after ride complete; money goes to single PayU account

        try {
            eventProducer.sendRideCompleted(saved);
        } catch (Exception e) {
            log.warn("Failed to send ride-completed event for ride {}: {}", rideId, e.getMessage());
        }

        try {
            notificationService.notifyRideStatusChange(
                    rideId, "RIDE_COMPLETED", saved.getUserId(), saved.getDriverId());
        } catch (Exception e) {
            log.warn("Failed to notify ride completion for ride {}: {}", rideId, e.getMessage());
        }

        return saved;
    }

    @Transactional
    public void sendRideCompletedEvent(String rideId) {
        rideRepository.findById(rideId).ifPresent(eventProducer::sendRideCompleted);
    }

    public Map<String, Object> buildReceipt(Ride ride) {
        return Map.of(
                "ride_id", ride.getId(),
                "pickup", ride.getPickupAddress(),
                "drop", ride.getDropAddress(),
                "distance_km", ride.getRideKm(),
                "fare", ride.getFare(),
                "date", ride.getCompletedAt()
        );
    }

    private static boolean isValidUUID(String s) {
        if (s == null || s.isBlank()) return false;
        try {
            java.util.UUID.fromString(s);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
