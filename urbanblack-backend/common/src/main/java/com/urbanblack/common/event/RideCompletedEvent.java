package com.urbanblack.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Kafka event published when a ride is completed.
 * Consumed by payment-service to create payment record (PayU UPI).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RideCompletedEvent {
    private String rideId;
    private String userId;
    private String driverId;
    private BigDecimal fare;
    private Double rideKm;
    private String pickupAddress;
    private String dropAddress;
    private LocalDateTime completedAt;
}
