package EmployeeDetails_Service.EmployeeDetails_Service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request body for sending OTP")
public class OtpSendRequest {

    @Schema(description = "Document number to send OTP for", example = "123456789012")
    private String identifier;
}
