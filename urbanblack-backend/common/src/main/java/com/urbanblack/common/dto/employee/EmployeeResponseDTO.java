package com.urbanblack.common.dto.employee;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeResponseDTO {
    private Long id;
    private String fullName;
    private String email;
    private String mobile;
    private String role;
    private String accountStatus;
    private String verificationStatus;
    private String dob;
    private String registrationDate;
    private String address;
    private String city;
    private String state;
    private String pincode;
    private String aadharNo;
    private String licenseNo;
    private String branchName;
    private String ifscCode;
    private String accountNo;
    private String recentDegree;
    private String yearOfPassing;
    private String percentage;
    private String experience;
    private String medicalInsuranceNumber;
}

