package com.urbanblack.walletservice.controller;

import com.urbanblack.walletservice.entity.Wallet;
import com.urbanblack.walletservice.service.WalletService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @GetMapping
    public Wallet getBalance(@RequestParam Long userId) {
        return walletService.getWalletByUserId(userId);
    }

    @PostMapping("/debit/ride")
    public Wallet debitForRide(
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody DebitRideRequest request) {
        return walletService.debitForRide(
                request.getUserId(),
                request.getAmount(),
                request.getRideId(),
                idempotencyKey
        );
    }

    @PostMapping("/debit/overuse")
    public Wallet debitForOveruse(
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody DebitOveruseRequest request) {
        return walletService.debitForOveruse(
                request.getUserId(),
                request.getAmount(),
                request.getShiftId(),
                idempotencyKey
        );
    }

    @Data
    public static class DebitRideRequest {
        private Long userId;
        private BigDecimal amount;
        private String rideId;
    }

    @Data
    public static class DebitOveruseRequest {
        private Long userId;
        private BigDecimal amount;
        private String shiftId;
    }
}
