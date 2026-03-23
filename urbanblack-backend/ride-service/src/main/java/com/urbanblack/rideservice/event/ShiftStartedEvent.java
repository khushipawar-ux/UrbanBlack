package com.urbanblack.rideservice.event;

import lombok.*;

import java.time.LocalDateTime;

/**
 * Mirror of the driver-service {@code ShiftStartedEvent}.
 * Deserialized from the Kafka topic {@code driver.shift.started} by
 * {@link com.urbanblack.rideservice.kafka.ShiftEventConsumer}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShiftStartedEvent {

    private String shiftId;
    private String driverId;
    private Double latitude;
    private Double longitude;
    private LocalDateTime clockInTime;
    private double freeKmQuota;
}
