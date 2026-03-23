package com.urbanblack.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Kafka event when payment for a ride succeeds.
 * Consumed by ride-service to trigger reward tree.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RidePaymentCompletedEvent {
    private String rideId;
    private String userId;
}
