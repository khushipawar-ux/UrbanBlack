package com.urbanblack.paymentservice.kafka;

import com.urbanblack.common.event.RideCompletedEvent;
import com.urbanblack.paymentservice.entity.Payment;
import com.urbanblack.paymentservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Consumes ride.completed Kafka event.
 * Creates Payment record (PENDING, UPI only) for user to pay via PayU.
 * Money goes to single PayU account; driverId stored for admin collection analytics.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RideCompletedConsumer {

    private final PaymentRepository paymentRepository;

    @KafkaListener(
            topics = "ride.completed",
            groupId = "urbanblack-payment-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void onRideCompleted(RideCompletedEvent event) {
        log.info("[Kafka] Received ride.completed | rideId={} userId={} driverId={} fare={}",
                event.getRideId(), event.getUserId(), event.getDriverId(), event.getFare());

        if (event.getFare() == null || event.getFare().compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("[Kafka] Skipping payment creation - invalid fare for ride {}", event.getRideId());
            return;
        }

        if (paymentRepository.findTopByRideIdOrderByCreatedAtDesc(event.getRideId()).isPresent()) {
            log.info("[Kafka] Payment already exists for ride {}", event.getRideId());
            return;
        }

        Long userId;
        try {
            userId = Long.parseLong(event.getUserId());
        } catch (NumberFormatException e) {
            log.error("[Kafka] Invalid userId for ride {}: {}", event.getRideId(), event.getUserId());
            return;
        }

        Payment payment = Payment.builder()
                .txnId("PAYU_" + event.getRideId() + "_" + UUID.randomUUID().toString().substring(0, 8))
                .rideId(event.getRideId())
                .userId(userId)
                .driverId(event.getDriverId())
                .totalAmount(event.getFare())
                .walletAmount(BigDecimal.ZERO)
                .onlineAmount(event.getFare())
                .paymentGateway("PAYU")
                .paymentMode("UPI")
                .status(Payment.Status.PENDING)
                .build();

        paymentRepository.save(payment);
        log.info("[Kafka] Created Payment {} for ride {} (driverId={})", payment.getId(), event.getRideId(), event.getDriverId());
    }
}
