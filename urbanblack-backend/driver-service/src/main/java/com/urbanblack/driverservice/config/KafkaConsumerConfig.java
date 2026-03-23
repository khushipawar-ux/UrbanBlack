package com.urbanblack.driverservice.config;

import org.springframework.context.annotation.Configuration;

/**
 * Kafka consumer is configured entirely via application.yaml.
 * Spring Boot auto-configures the ConsumerFactory and
 * ConcurrentKafkaListenerContainerFactory from those properties.
 * No additional Java config is needed — adding any here would conflict.
 */
@Configuration
public class KafkaConsumerConfig {
    // Intentionally empty — Spring Boot autoconfiguration handles everything.
}

