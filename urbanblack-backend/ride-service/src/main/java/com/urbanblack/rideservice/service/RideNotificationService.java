package com.urbanblack.rideservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Pushes ride lifecycle events to connected clients via WebSocket (STOMP).
 *
 * <p>Topics:
 * <ul>
 *   <li>{@code /topic/driver/{driverId}/ride-offers} — new ride offer for a specific driver</li>
 *   <li>{@code /topic/ride/{rideId}/status} — ride status updates for the rider (user)</li>
 * </ul>
 *
 * <p>Driver live location during a ride is already handled by {@link DriverLocationService}
 * on {@code /topic/ride/{rideId}/driver-location}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RideNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Notifies nearby drivers about a new ride request.
     * Each driver receives the offer on their personal topic.
     */
    public void notifyNearbyDrivers(String rideId,
                                    double pickupLat,
                                    double pickupLng,
                                    String pickupAddress,
                                    double dropLat,
                                    double dropLng,
                                    String dropAddress,
                                    double rideKm,
                                    double fare,
                                    String customerName,
                                    Double customerRating,
                                    String scheduledTime,
                                    List<NearestDriverService.NearestDriverResult> nearbyDrivers) {
        if (nearbyDrivers == null || nearbyDrivers.isEmpty()) {
            log.info("[RideNotify] No nearby drivers to notify for ride {}", rideId);
            return;
        }

        Map<String, Object> rideOffer = Map.ofEntries(
            Map.entry("rideId", rideId),
            Map.entry("event", "NEW_RIDE_OFFER"),
            Map.entry("pickupLat", pickupLat),
            Map.entry("pickupLng", pickupLng),
            Map.entry("pickupAddress", pickupAddress != null ? pickupAddress : ""),
            Map.entry("dropLat", dropLat),
            Map.entry("dropLng", dropLng),
            Map.entry("dropAddress", dropAddress != null ? dropAddress : ""),
            Map.entry("rideKm", rideKm),
            Map.entry("fare", fare),
            Map.entry("customerName", customerName != null ? customerName : ""),
            Map.entry("customerRating", customerRating != null ? customerRating : 0.0),
            Map.entry("scheduledTime", scheduledTime != null ? scheduledTime : "Now")
        );

        for (NearestDriverService.NearestDriverResult driver : nearbyDrivers) {
            String topic = "/topic/driver/" + driver.getDriverId() + "/ride-offers";
            messagingTemplate.convertAndSend(topic, rideOffer);
            log.info("[RideNotify] Sent ride offer {} to driver {} on {}", rideId, driver.getDriverId(), topic);
        }
    }

    /**
     * Notifies the rider about a ride status change (DRIVER_ACCEPTED, ARRIVED, STARTED, COMPLETED, CANCELLED).
     * Also notifies the driver on cancellation.
     *
     * When {@code otp} is non-null (i.e. on DRIVER_ACCEPTED), it is included in the
     * payload so the passenger can display it immediately without an extra REST poll.
     */
    public void notifyRideStatusChange(String rideId,
                                        String status,
                                        String userId,
                                        String driverId) {
        notifyRideStatusChange(rideId, status, userId, driverId, null);
    }

    public void notifyRideStatusChange(String rideId,
                                        String status,
                                        String userId,
                                        String driverId,
                                        String otp) {
        java.util.Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("rideId", rideId);
        payload.put("event", "STATUS_CHANGED");
        payload.put("status", status);
        payload.put("driverId", driverId != null ? driverId : "");
        if (otp != null && !otp.isBlank()) {
            payload.put("otp", otp);
        }

        // Always notify the rider
        String riderTopic = "/topic/ride/" + rideId + "/status";
        messagingTemplate.convertAndSend(riderTopic, payload);
        log.info("[RideNotify] Status {} sent to rider on {}", status, riderTopic);

        // On cancellation, also notify assigned driver
        if ("CANCELLED".equals(status) && driverId != null) {
            String driverTopic = "/topic/driver/" + driverId + "/ride-offers";
            messagingTemplate.convertAndSend(driverTopic, Map.of(
                    "rideId", rideId,
                    "event", "RIDE_CANCELLED"
            ));
            log.info("[RideNotify] Cancellation sent to driver {} on {}", driverId, driverTopic);
        }
    }
}
