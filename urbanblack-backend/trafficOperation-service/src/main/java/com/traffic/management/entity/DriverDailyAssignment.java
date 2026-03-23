package com.traffic.management.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "driver_daily_assignment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverDailyAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "assignment_id")
    private Long assignmentId;

    @Column(name = "assignment_date", nullable = false)
    private LocalDate assignmentDate;

    @Column(name = "driver_id", nullable = false)
    private Long driverId; // Reference to Employee from EmployeeDetails-Service

    @Column(name = "cab_id", nullable = false)
    private Long cabId; // Reference to Vehicle from CabRegistration-Service

    @ManyToOne
    @JoinColumn(name = "shift_id", nullable = false)
    private ShiftMaster shift;

    @ManyToOne
    @JoinColumn(name = "center_point_id", nullable = false)
    private CenterPoint centerPoint;

    @ManyToOne
    @JoinColumn(name = "depot_id", nullable = false)
    private Depot depot;

    @Column(name = "status", length = 20)
    private String status; // e.g., PENDING, ONGOING, COMPLETED, CANCELLED

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = "PENDING";
        }
    }
}
