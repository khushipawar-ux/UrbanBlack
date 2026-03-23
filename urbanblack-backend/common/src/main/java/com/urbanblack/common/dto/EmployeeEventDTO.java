package com.urbanblack.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EmployeeEventDTO {

    // ── Core ──────────────────────────────────────────────────────────
    private Long   id;           // employee table PK (used as employeeId in Driver)
    private String name;         // fullName
    private String status;       // AccountStatus (ACTIVE / INACTIVE)
    private String role;         // EmployeeRole enum name (e.g. "DRIVER")

    // ── Contact ───────────────────────────────────────────────────────
    private String email;
    private String mobile;       // mapped to Driver.phoneNumber

    // ── Driver-specific ───────────────────────────────────────────────
    private String    licenseNumber;   // from DrivingLicense entity
    private LocalDate dateOfJoining;   // employee.registrationDate
    private String    profileImage;    // from Aadhaar entity (optional)
}

