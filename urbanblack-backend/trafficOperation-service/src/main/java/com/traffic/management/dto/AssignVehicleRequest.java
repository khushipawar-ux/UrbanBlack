package com.traffic.management.dto;

import lombok.*;
import java.time.LocalDate;
import java.util.List;

/**
 * Request DTO for Assign Vehicles to Depot.
 *
 * Fields:
 *  - depotId          : ID of the target Depot
 *  - registrationDate : Date of assignment (defaults to today if null)
 *  - vehicleIds       : List of Vehicle IDs to assign (multiple cabs at a time)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignVehicleRequest {
    private Long depotId;
    private LocalDate registrationDate;
    private List<Long> vehicleIds;
}
