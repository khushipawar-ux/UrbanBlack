package com.urbanblack.rideservice.kafka;

import com.urbanblack.rideservice.event.ShiftEndedEvent;
import com.urbanblack.rideservice.event.ShiftStartedEvent;
import com.urbanblack.rideservice.service.ShiftService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Listens to driver shift lifecycle events produced by driver-service.
 * Delegates to {@link ShiftService} to initialise or finalise ride-service records.
 * <p>
 * This is the <em>only</em> entry point for shift state in ride-service –
 * no REST shift endpoints exist in this service.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ShiftEventConsumer {

    private final ShiftService shiftService;

    /**
     * Triggered when a driver clocks in.
     * Creates a {@code DriverShift} record in the ride-service database.
     */
    @KafkaListener(
            topics = "driver.shift.started",
            groupId = "urbanblack-ride-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onShiftStarted(ShiftStartedEvent event) {
        log.info("[Kafka] Received driver.shift.started | driverId={} shiftId={}",
                event.getDriverId(), event.getShiftId());
        try {
            shiftService.onShiftStarted(event);
        } catch (Exception e) {
            log.error("[Kafka] Error handling driver.shift.started for driverId={}: {}",
                    event.getDriverId(), e.getMessage(), e);
        }
    }

    /**
     * Triggered when a driver clocks out (or shift auto-completes).
     * Closes the {@code DriverShift} record and persists the daily {@code DriverKmLedger}.
     */
    @KafkaListener(
            topics = "driver.shift.ended",
            groupId = "urbanblack-ride-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onShiftEnded(ShiftEndedEvent event) {
        log.info("[Kafka] Received driver.shift.ended | driverId={} shiftId={}",
                event.getDriverId(), event.getShiftId());
        try {
            shiftService.onShiftEnded(event);
        } catch (Exception e) {
            log.error("[Kafka] Error handling driver.shift.ended for driverId={}: {}",
                    event.getDriverId(), e.getMessage(), e);
        }
    }
}
