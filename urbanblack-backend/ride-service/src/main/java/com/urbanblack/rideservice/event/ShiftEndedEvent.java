package com.urbanblack.rideservice.event;

import lombok.*;

import java.time.LocalDateTime;

/**
 * Mirror of the driver-service {@code ShiftEndedEvent}.
 * Deserialized from the Kafka topic {@code driver.shift.ended} by
 * {@link com.urbanblack.rideservice.kafka.ShiftEventConsumer}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShiftEndedEvent {

    private String shiftId;
    private String driverId;
    private LocalDateTime clockOutTime;
    private long totalActiveMinutes;
}
