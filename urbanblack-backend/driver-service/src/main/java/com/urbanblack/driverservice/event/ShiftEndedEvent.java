package com.urbanblack.driverservice.event;

import lombok.*;

import java.time.LocalDateTime;

/**
 * Published to topic {@code driver.shift.ended} when a driver clocks out (or
 * the shift
 * auto-completes after 12 hours).
 * Consumed by ride-service to finalise its DriverShift and compute the
 * DriverKmLedger.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShiftEndedEvent {

    /** Unique shift identifier (same value used in {@link ShiftStartedEvent}). */
    private String shiftId;

    /** Driver UUID from the drivers table. */
    private String driverId;

    /** IST timestamp when the shift was completed. */
    private LocalDateTime clockOutTime;

    /** Total minutes the driver was ONLINE during this shift. */
    private long totalActiveMinutes;
}
