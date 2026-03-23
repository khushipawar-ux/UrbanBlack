package com.urbanblack.driverservice.dto;

import lombok.*;

/**
 * Lightweight projection of driver state exposed by driver-service for
 * inter-service calls.
 * Consumed by ride-service via Feign to check shift status and current location
 * before
 * assigning a ride.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverSummaryDto {

    /** Driver UUID (internal id). */
    private String driverId;

    /** Employee ID – used as driverId in ride/payment flow (admin assigns cab/depot/shift by empId). */
    private String employeeId;

    /** Driver's first name (for rider display). */
    private String firstName;

    /** Driver's last name (for rider display). */
    private String lastName;

    /** Active shift ID (null if driver has no active shift). */
    private String shiftId;

    /** "ACTIVE", "COMPLETED", or null if no shift. */
    private String shiftStatus;

    /** True if the driver is currently ONLINE within an active shift. */
    private Boolean isOnline;

    /** Last known latitude (null until a location update is received). */
    private Double latitude;

    /** Last known longitude (null until a location update is received). */
    private Double longitude;
}
