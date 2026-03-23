package com.urbanblack.driverservice.event;

import lombok.*;

import java.time.LocalDateTime;

/**
 * Published to topic {@code driver.shift.started} when a driver successfully
 * clocks in.
 * Consumed by ride-service to initialise its DriverShift and KM-ledger records.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShiftStartedEvent {

    /** Unique shift identifier (generated in driver-service). */
    private String shiftId;

    /** Driver UUID from the drivers table. */
    private String driverId;

    /** Driver latitude at clock-in time (nullable – may not be available yet). */
    private Double latitude;

    /** Driver longitude at clock-in time (nullable – may not be available yet). */
    private Double longitude;

    /** IST timestamp when the driver clocked in. */
    private LocalDateTime clockInTime;

    /**
     * Free-roaming KM quota for this shift (default: 12 km).
     * Ride-service uses this to initialise the {@code freeKmQuota} field on
     * DriverShift.
     */
    private double freeKmQuota;
}
