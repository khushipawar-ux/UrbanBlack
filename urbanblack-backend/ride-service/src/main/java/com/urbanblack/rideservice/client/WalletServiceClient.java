package com.urbanblack.rideservice.client;

import lombok.Builder;
import lombok.Data;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.math.BigDecimal;
import java.util.Map;

@FeignClient(name = "wallet-service")
public interface WalletServiceClient {

    @GetMapping("/api/v1/admin/wallet/summary")
    Map<String, Object> getAdminSummary();


    @PostMapping("/api/v1/wallet/debit/ride")
    Object debitForRide(
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @RequestBody DebitRideRequest request
    );

    @PostMapping("/api/v1/wallet/debit/overuse")
    Object debitForOveruse(
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @RequestBody DebitOveruseRequest request
    );

    @Data
    @Builder
    class DebitRideRequest {
        private Long userId;
        private BigDecimal amount;
        private String rideId;
    }

    @Data
    @Builder
    class DebitOveruseRequest {
        private Long userId;
        private BigDecimal amount;
        private String shiftId;
    }
}
