package com.urbanblack.paymentservice.service;

import com.urbanblack.common.event.RidePaymentCompletedEvent;
import com.urbanblack.paymentservice.client.RideClient;
import com.urbanblack.paymentservice.dto.RidePaymentUpdateRequest;
import com.urbanblack.paymentservice.entity.Payment;
import com.urbanblack.paymentservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * PayU UPI-only payment. Money goes to single PayU account.
 * Payment record created by Kafka on ride.completed.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private static final String TOPIC_RIDE_PAYMENT_COMPLETED = "ride.payment.completed";

    private final PaymentRepository paymentRepository;
    private final RideClient rideClient;
    private final PayuFormService payuFormService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Get PayU form for ride payment (UPI only).
     * Payment must exist (created by Kafka on ride complete).
     */
    public Map<String, String> initiateForRide(String rideId, Long userId, String userEmail, String userName) {
        Payment payment = paymentRepository.findTopByRideIdOrderByCreatedAtDesc(rideId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found for ride. Ensure ride is completed."));

        if (!payment.getUserId().equals(userId)) {
            throw new IllegalArgumentException("User mismatch");
        }
        if (payment.getStatus() != Payment.Status.PENDING) {
            throw new IllegalStateException("Payment already " + payment.getStatus());
        }

        Map<String, String> form = payuFormService.buildPayuForm(payment, userEmail, userName);
        form.put("payuUrl", payuFormService.getPayuUrl());
        return form;
    }

    @Transactional
    public Payment confirm(String paymentId, String gatewayTxnId, boolean success) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));

        if (payment.getStatus() == Payment.Status.SUCCESS || payment.getStatus() == Payment.Status.FAILED) {
            return payment;
        }

        payment.setGatewayTxnId(gatewayTxnId);
        payment.setUpdatedAt(LocalDateTime.now());

        if (!success) {
            payment.setStatus(Payment.Status.FAILED);
            Payment saved = paymentRepository.save(payment);
            rideClient.updateRidePayment(payment.getRideId(), RidePaymentUpdateRequest.builder()
                    .walletUsed(BigDecimal.ZERO)
                    .onlinePaid(payment.getTotalAmount())
                    .paymentStatus("FAILED")
                    .build());
            return saved;
        }

        payment.setStatus(Payment.Status.SUCCESS);
        Payment saved = paymentRepository.save(payment);

        rideClient.updateRidePayment(payment.getRideId(), RidePaymentUpdateRequest.builder()
                .walletUsed(BigDecimal.ZERO)
                .onlinePaid(payment.getTotalAmount())
                .paymentStatus("PAID")
                .build());

        RidePaymentCompletedEvent event = RidePaymentCompletedEvent.builder()
                .rideId(payment.getRideId())
                .userId(String.valueOf(payment.getUserId()))
                .build();
        kafkaTemplate.send(TOPIC_RIDE_PAYMENT_COMPLETED, payment.getRideId(), event);
        log.info("[Payment] Sent ride.payment.completed for ride {}", payment.getRideId());

        return saved;
    }

    @Transactional
    public Payment confirmByTxnId(String txnId, String gatewayTxnId, boolean success) {
        Payment payment = paymentRepository.findByTxnId(txnId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found for txnId"));
        return confirm(payment.getId(), gatewayTxnId, success);
    }
}
