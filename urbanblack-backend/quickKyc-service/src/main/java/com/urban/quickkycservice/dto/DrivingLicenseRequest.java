package com.urban.quickkycservice.dto;

import lombok.Data;

@Data
public class DrivingLicenseRequest {
    private String id_number;  // Driving License number
    private String dob;        // Date of birth in YYYY-MM-DD format (optional but used by QuickEKYC)
}
