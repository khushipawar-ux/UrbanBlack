package com.urbanblack.paymentservice.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class WalletBalanceResponse {
    private Long id;
    private Long userId;
    private BigDecimal balance;
    private BigDecimal totalEarned;
    private BigDecimal totalSpent;
    private Boolean frozen;
}

