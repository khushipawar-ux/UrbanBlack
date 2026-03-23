package com.urbanblack.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to send employee credentials email")
public class CredentialEmailRequest {

    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String fullName;

    @NotBlank
    private String role;

    @NotBlank
    private String username;

    @NotBlank
    private String tempPassword;

    private String employeeId;

    private String designation;
    private Integer durationMonths;
    private Double inHandSalary;
    private Integer monthlyOff;
    private String medicalInsuranceNumber;
}
