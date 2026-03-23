package com.urbanblack.driverservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "drivers", uniqueConstraints = {
        @UniqueConstraint(columnNames = "email"),
        @UniqueConstraint(columnNames = "phone_number")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Driver {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(name = "phone_number", unique = true)
    private String phoneNumber;

    private String firstName;
    private String lastName;
    private String profileImage;

    private boolean isActive;
    private boolean isVerified;

    private String city;
    private String language;

    private String employeeId;
    private String depotName;
    private String licenseNumber;

    private Double rating;
    private Integer totalTrips;
    private Double totalDistance;

    // ── Real-time location ──────────────────────────────────────────
    // Moved to Shift entity (clockIn / clockOut location tracking)
    @Enumerated(EnumType.STRING)
    private DriverStatus status;

    private LocalDate dateOfJoining;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}