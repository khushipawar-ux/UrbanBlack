package com.traffic.management.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

/**
 * Entity for the assign_manager table.
 *
 * Assign Manager to Depot
 * ─────────────────────────────────────────
 *  Column                   | Java Field
 * ─────────────────────────────────────────
 *  assign_manager_to_depot_id | id  (PK)
 *  registration_date          | registrationDate
 *  manager_id                 | managerId  (Employee ID of the Depot Manager)
 *  depot_id                   | depotId
 * ─────────────────────────────────────────
 */
@Entity
@Table(name = "assign_manager")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignManager {

    /** AssignManager_toDepot_ID */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "assign_manager_to_depot_id")
    private Long id;

    /** Registration Date */
    @Column(name = "registration_date", nullable = false)
    private LocalDate registrationDate;

    /** Select Depot Manager ID (Employee ID from EmployeeDetails-Service) */
    @Column(name = "manager_id", nullable = false)
    private Long managerId;

    /** Depot ID */
    @Column(name = "depot_id", nullable = false)
    private Long depotId;
}
