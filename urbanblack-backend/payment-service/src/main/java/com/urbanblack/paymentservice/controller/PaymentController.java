package com.urbanblack.paymentservice.controller;

import com.urbanblack.paymentservice.entity.Payment;
import com.urbanblack.paymentservice.service.PaymentService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/initiate")
    public ResponseEntity<InitiateResponse> initiate(@RequestBody InitiateRequest request) {
        Map<String, String> form = paymentService.initiateForRide(
                request.getRideId(),
                request.getUserId(),
                request.getUserEmail(),
                request.getUserName());
        return ResponseEntity.ok(InitiateResponse.builder()
                .rideId(request.getRideId())
                .payuUrl(form.get("payuUrl"))
                .payuForm(form)
                .paymentMode("UPI")
                .build());
    }

    @PostMapping("/confirm")
    public ResponseEntity<Payment> confirm(@RequestBody ConfirmRequest request) {
        return ResponseEntity.ok(paymentService.confirm(
                request.getPaymentId(),
                request.getGatewayTxnId(),
                Boolean.TRUE.equals(request.getSuccess())
        ));
    }

    @Data
    public static class InitiateRequest {
        @NotBlank
        private String rideId;
        @NotNull
        private Long userId;
        private String userEmail;
        private String userName;
    }

    @Data
    @Builder
    public static class InitiateResponse {
        private String rideId;
        private String payuUrl;
        private Map<String, String> payuForm;
        private String paymentMode;
    }

    @Data
    public static class ConfirmRequest {
        @NotBlank
        private String paymentId;
        private String gatewayTxnId;
        @NotNull
        private Boolean success;
    }
}

