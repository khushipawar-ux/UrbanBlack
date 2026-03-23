package EmployeeDetails_Service.EmployeeDetails_Service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request body for verifying OTP")
public class OtpVerifyRequest {

    @Schema(description = "Document number that OTP was sent for", example = "123456789012")
    private String identifier;

    @Schema(description = "6-digit OTP received", example = "482931")
    private String otp;
}
