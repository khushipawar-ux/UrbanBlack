package com.urbanblack.driverservice.kafka;

import com.urbanblack.driverservice.event.ShiftEndedEvent;
import com.urbanblack.driverservice.event.ShiftStartedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes driver shift lifecycle events to Kafka.
 * <p>
 * Topics:
 * <ul>
 * <li>{@code driver.shift.started} – emitted when a driver successfully clocks
 * in</li>
 * <li>{@code driver.shift.ended} – emitted when a shift completes (manual
 * clock-out or auto)</li>
 * </ul>
 * Ride-service consumes both topics to manage its own DriverShift and
 * DriverKmLedger records.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ShiftEventProducer {

    public static final String TOPIC_SHIFT_STARTED = "driver.shift.started";
    public static final String TOPIC_SHIFT_ENDED = "driver.shift.ended";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Publishes a {@link ShiftStartedEvent} keyed by driverId.
     */
    public void sendShiftStarted(ShiftStartedEvent event) {
        try {
            kafkaTemplate.send(TOPIC_SHIFT_STARTED, event.getDriverId(), event);
            log.info("[Kafka] Published {} | driverId={} shiftId={}",
                    TOPIC_SHIFT_STARTED, event.getDriverId(), event.getShiftId());
        } catch (Exception e) {
            log.error("[Kafka] Failed to publish {}: {}", TOPIC_SHIFT_STARTED, e.getMessage());
        }
    }

    /**
     * Publishes a {@link ShiftEndedEvent} keyed by driverId.
     */
    public void sendShiftEnded(ShiftEndedEvent event) {
        try {
            kafkaTemplate.send(TOPIC_SHIFT_ENDED, event.getDriverId(), event);
            log.info("[Kafka] Published {} | driverId={} shiftId={}",
                    TOPIC_SHIFT_ENDED, event.getDriverId(), event.getShiftId());
        } catch (Exception e) {
            log.error("[Kafka] Failed to publish {}: {}", TOPIC_SHIFT_ENDED, e.getMessage());
        }
    }
}
