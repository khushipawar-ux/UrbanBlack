package com.urban.quickkycservice.dto;

import lombok.Data;

@Data
public class OtpVerificationRequest {
    private String request_id;
    private String id_number;
    private String otp;
}
