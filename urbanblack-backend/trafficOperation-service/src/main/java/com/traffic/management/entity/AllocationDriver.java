package com.traffic.management.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.List;

/**
 * Entity for the Assign_driver table.
 * Stores a single batch assignment of multiple Drivers (Employees) to a Depot.
 *
 * Table: assign_driver
 * Columns:
 *   - assign_drivers_to_depot_id  (PK, auto-generated)
 *   - depot_id                    (ID of the Depot)
 *   - registration_date           (date of driver assignment)
 *   - drivers_count               (number of drivers assigned in this batch)
 *   - employee_ids                (comma-separated list of Employee IDs)
 */
@Entity
@Table(name = "assign_driver")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AllocationDriver {

    /** AssignDriversToDepot_Id */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "assign_drivers_to_depot_id")
    private Long id;

    /** Depot ID */
    @Column(name = "depot_id")
    private Long depotId;

    /** Registration Date */
    @Column(name = "registration_date")
    private LocalDate registrationDate;

    /** Drivers Count (auto-computed from employeeIds list size) */
    @Column(name = "drivers_count")
    private Integer driversCount;

    /**
     * Employee IDs – stored as a comma-separated string.
     * Multiple Employees can be chosen at a time.
     */
    @Column(name = "employee_ids", columnDefinition = "TEXT")
    private String employeeIdsRaw;

    // -----------------------------------------------------------------------
    // Transient helpers – not persisted, convenient for service layer
    // -----------------------------------------------------------------------

    @Transient
    public List<Long> getEmployeeIds() {
        if (employeeIdsRaw == null || employeeIdsRaw.isBlank()) return List.of();
        return java.util.Arrays.stream(employeeIdsRaw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .toList();
    }

    @Transient
    public void setEmployeeIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            this.employeeIdsRaw = "";
            this.driversCount = 0;
        } else {
            this.employeeIdsRaw = ids.stream()
                    .map(String::valueOf)
                    .reduce((a, b) -> a + "," + b)
                    .orElse("");
            this.driversCount = ids.size();
        }
    }
}
