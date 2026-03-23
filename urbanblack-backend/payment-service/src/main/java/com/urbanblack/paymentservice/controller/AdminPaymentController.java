package com.urbanblack.paymentservice.controller;

import com.urbanblack.paymentservice.entity.Payment;
import com.urbanblack.paymentservice.repository.PaymentRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Admin APIs for driver collection analytics.
 * Shows which driver's rides generated how much revenue (all to single PayU account).
 */
@RestController
@RequestMapping("/api/v1/admin/payments")
@RequiredArgsConstructor
public class AdminPaymentController {

    private final PaymentRepository paymentRepository;

    @GetMapping("/driver-collections")
    public ResponseEntity<List<DriverCollectionSummary>> getDriverCollections() {
        List<Object[]> rows = paymentRepository.findDriverCollectionSummary();
        List<DriverCollectionSummary> result = rows.stream()
                .map(row -> DriverCollectionSummary.builder()
                        .driverId((String) row[0])
                        .totalCollected((BigDecimal) row[1])
                        .rideCount(((Number) row[2]).longValue())
                        .build())
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/driver-collections/{driverId}")
    public ResponseEntity<DriverCollectionDetail> getDriverCollectionDetail(@PathVariable String driverId) {
        List<Payment> payments = paymentRepository.findByDriverIdAndStatusOrderByCreatedAtDesc(driverId, Payment.Status.SUCCESS);
        BigDecimal total = payments.stream()
                .map(Payment::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<DriverCollectionDetail.RidePayment> ridePayments = payments.stream()
                .map(p -> DriverCollectionDetail.RidePayment.builder()
                        .rideId(p.getRideId())
                        .amount(p.getTotalAmount())
                        .createdAt(p.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(DriverCollectionDetail.builder()
                .driverId(driverId)
                .totalCollected(total)
                .rideCount((long) payments.size())
                .payments(ridePayments)
                .build());
    }

    @Data
    @Builder
    public static class DriverCollectionSummary {
        private String driverId;
        private BigDecimal totalCollected;
        private Long rideCount;
    }

    @Data
    @Builder
    public static class DriverCollectionDetail {
        private String driverId;
        private BigDecimal totalCollected;
        private Long rideCount;
        private List<RidePayment> payments;

        @Data
        @Builder
        public static class RidePayment {
            private String rideId;
            private BigDecimal amount;
            private java.time.LocalDateTime createdAt;
        }
    }
}
