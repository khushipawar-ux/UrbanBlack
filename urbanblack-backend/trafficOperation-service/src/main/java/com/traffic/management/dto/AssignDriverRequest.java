package com.traffic.management.dto;

import lombok.*;
import java.time.LocalDate;
import java.util.List;

/**
 * Request DTO for Assign Drivers (Employees) to Depot.
 *
 * Fields:
 *  - depotId          : ID of the target Depot
 *  - registrationDate : Date of assignment (defaults to today if null)
 *  - employeeIds      : List of Employee IDs to assign (multiple drivers at a time)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignDriverRequest {
    private Long depotId;
    private LocalDate registrationDate;
    private List<Long> employeeIds;
}
