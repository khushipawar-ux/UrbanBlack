package com.urban.quickkycservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Request body for testing SMS delivery")
public class SmsTestRequest {
    
    @Schema(description = "10-digit mobile number to send SMS to", example = "9876543210")
    private String mobile;
}
