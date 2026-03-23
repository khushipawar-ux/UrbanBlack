package com.urbanblack.paymentservice.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class RidePaymentUpdateRequest {
    private BigDecimal walletUsed;
    private BigDecimal onlinePaid;
    private String paymentStatus; // "PENDING" | "PAID" | "FAILED"
}

