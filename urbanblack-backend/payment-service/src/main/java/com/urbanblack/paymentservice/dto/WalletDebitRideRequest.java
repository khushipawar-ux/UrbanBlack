package com.urbanblack.paymentservice.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class WalletDebitRideRequest {
    private Long userId;
    private BigDecimal amount;
    private String rideId;
}

