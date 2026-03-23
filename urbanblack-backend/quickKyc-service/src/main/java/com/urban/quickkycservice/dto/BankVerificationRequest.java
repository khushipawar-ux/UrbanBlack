package com.urban.quickkycservice.dto;

import lombok.Data;

@Data
public class BankVerificationRequest {
    private String account_number;
    private String ifsc_code;
}
