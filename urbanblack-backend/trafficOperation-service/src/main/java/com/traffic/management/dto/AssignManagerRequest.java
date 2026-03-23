package com.traffic.management.dto;

import lombok.*;
import java.time.LocalDate;

/**
 * Request DTO for Assign Manager to Depot.
 *
 * Fields:
 *  - registrationDate : Date of assignment (defaults to today if null)
 *  - managerId        : Employee ID of the selected Depot Manager
 *  - depotId          : ID of the target Depot
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignManagerRequest {
    private LocalDate registrationDate;
    private Long managerId;
    private Long depotId;
}
