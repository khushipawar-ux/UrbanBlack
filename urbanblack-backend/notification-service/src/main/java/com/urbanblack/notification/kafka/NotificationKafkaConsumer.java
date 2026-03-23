package com.urbanblack.notification.kafka;

import com.urbanblack.notification.dto.EmailRequest;
import com.urbanblack.notification.service.EmailNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * NotificationKafkaConsumer
 *
 * Listens to Kafka topics and triggers notifications asynchronously.
 *
 * Topics:
 *   - notification.email       → Generic email (any type)
 *   - notification.credentials → Employee credential email
 *
 * Other microservices publish to these topics instead of calling the
 * notification REST API directly, enabling full decoupling.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationKafkaConsumer {

    private final EmailNotificationService emailService;

    /**
     * Generic email notification topic.
     * Any service can publish an EmailRequest to this topic.
     */
    @KafkaListener(
        topics = "notification.email",
        groupId = "notification-service-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeEmailNotification(EmailRequest request) {
        log.info("📨 Kafka → notification.email received for: {}", request.getTo());
        emailService.sendAsync(request);
    }

    /**
     * Dedicated topic for employee onboarding credential emails.
     * Published by EmployeeDetails-Service after employee creation.
     */
    @KafkaListener(
        topics = "notification.credentials",
        groupId = "notification-service-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeCredentialNotification(EmailRequest request) {
        log.info("🔐 Kafka → notification.credentials received for: {}", request.getTo());
        emailService.sendAsync(request);
    }
}
