package com.urbanblack.paymentservice.client;

import com.urbanblack.paymentservice.dto.WalletBalanceResponse;
import com.urbanblack.paymentservice.dto.WalletDebitRideRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "walletClient", url = "${services.wallet.base-url}")
public interface WalletClient {

    @GetMapping("/api/v1/wallet")
    WalletBalanceResponse getWallet(@RequestParam("userId") Long userId);

    @PostMapping("/api/v1/wallet/debit/ride")
    WalletBalanceResponse debitForRide(
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody WalletDebitRideRequest request);
}

