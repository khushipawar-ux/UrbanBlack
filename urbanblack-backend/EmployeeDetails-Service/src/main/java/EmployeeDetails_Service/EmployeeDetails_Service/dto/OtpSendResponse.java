package EmployeeDetails_Service.EmployeeDetails_Service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response body for OTP send operations")
public class OtpSendResponse {

    @Schema(description = "Status message", example = "OTP sent successfully.")
    private String message;

    @Schema(description = "Document type the OTP was generated for", example = "AADHAAR")
    private String documentType;

    @Schema(description = "Masked identifier (last 4 digits)", example = "****9012")
    private String maskedIdentifier;

    @Schema(description = "The OTP code (dev mode only)", example = "482931")
    private String otp;
}
