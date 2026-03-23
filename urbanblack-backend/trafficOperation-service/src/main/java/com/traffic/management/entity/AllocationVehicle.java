package com.traffic.management.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.List;

/**
 * Entity for the Allocate_Vehicles table.
 * Stores a single batch assignment of multiple Vehicles to a Depot.
 *
 * Table: allocate_vehicles
 * Columns:
 *   - vehicle_to_depot_id  (PK, auto-generated)
 *   - depot_id             (ID of the Depot)
 *   - registration_date    (date of vehicle assignment)
 *   - vehicles_count       (number of vehicles assigned in this batch)
 *   - vehicle_ids          (comma-separated list of Vehicle IDs)
 */
@Entity
@Table(name = "allocate_vehicles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AllocationVehicle {

    /** VehicleToDepot_Id */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vehicle_to_depot_id")
    private Long id;

    /** Depot ID */
    @Column(name = "depot_id")
    private Long depotId;

    /** Registration Date */
    @Column(name = "registration_date")
    private LocalDate registrationDate;

    /** Vehicles Count (auto-computed from vehicleIds list size) */
    @Column(name = "vehicles_count")
    private Integer vehiclesCount;

    /**
     * Vehicle IDs – stored as a comma-separated string.
     * Multiple Vehicles can be chosen at a time.
     */
    @Column(name = "vehicle_ids", columnDefinition = "TEXT")
    private String vehicleIdsRaw;

    // -----------------------------------------------------------------------
    // Transient helpers – not persisted, convenient for service layer
    // -----------------------------------------------------------------------

    @Transient
    public List<Long> getVehicleIds() {
        if (vehicleIdsRaw == null || vehicleIdsRaw.isBlank()) return List.of();
        return java.util.Arrays.stream(vehicleIdsRaw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .toList();
    }

    @Transient
    public void setVehicleIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            this.vehicleIdsRaw = "";
            this.vehiclesCount = 0;
        } else {
            this.vehicleIdsRaw = ids.stream()
                    .map(String::valueOf)
                    .reduce((a, b) -> a + "," + b)
                    .orElse("");
            this.vehiclesCount = ids.size();
        }
    }
}
