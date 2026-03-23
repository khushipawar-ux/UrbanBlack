package com.urbanblack.rideservice.dto;

import lombok.*;

/**
 * Mirror of the driver-service {@code DriverSummaryDto}.
 * Populated via {@link com.urbanblack.rideservice.client.DriverServiceClient}
 * for synchronous shift-state and location lookups during ride assignment.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverSummaryDto {

    private String driverId;

    /** Employee ID for assignment lookup (traffic service uses driver_id = employee id). */
    private String employeeId;

    private String firstName;

    private String lastName;

    /** Active shift ID (null if driver has no active shift). */
    private String shiftId;

    /** "ACTIVE", "COMPLETED", or null. */
    private String shiftStatus;

    /** True if the driver is currently ONLINE. */
    private Boolean isOnline;

    /** Last known latitude (from driver-service Driver table). */
    private Double latitude;

    /** Last known longitude (from driver-service Driver table). */
    private Double longitude;
}
