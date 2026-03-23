package EmployeeDetails_Service.EmployeeDetails_Service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CredentialEmailRequest {
    private String email;
    private String fullName;
    private String role;
    private String username;
    private String tempPassword;
    private String employeeId;
    
    // Package Details
    private String designation;
    private Integer durationMonths;
    private Double inHandSalary;
    private Integer monthlyOff;
    private String medicalInsuranceNumber;
}
