package com.urbanblack.walletservice.controller;

import com.urbanblack.walletservice.entity.WalletTransaction;
import com.urbanblack.walletservice.service.WalletService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Collections;

@RestController
@RequestMapping("/internal/wallet")
@RequiredArgsConstructor
public class InternalWalletController {

    private final WalletService walletService;

    @PostMapping("/credit")
    public void creditUser(@RequestBody InternalCreditRequest request) {
        walletService.batchCredit(Collections.singletonList(Long.parseLong(request.getUserId())), request.getAmount());
    }

    @Data
    public static class InternalCreditRequest {
        private String userId;
        private BigDecimal amount;
    }
}
