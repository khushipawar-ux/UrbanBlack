package com.urbanblack.rideservice.kafka;

import com.urbanblack.common.event.RideCompletedEvent;
import com.urbanblack.rideservice.entity.Ride;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RideEventProducer {

    public static final String TOPIC_RIDE_REQUESTED = "ride.requested";
    public static final String TOPIC_RIDE_ACCEPTED = "ride.accepted";
    public static final String TOPIC_RIDE_CANCELLED = "ride.cancelled";
    public static final String TOPIC_RIDE_COMPLETED = "ride.completed";
    public static final String TOPIC_DRIVER_LOCATION_UPDATED = "driver.location.updated";
    public static final String TOPIC_DRIVER_SHIFT_STARTED = "driver.shift.started";
    public static final String TOPIC_DRIVER_SHIFT_ENDED = "driver.shift.ended";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendRideRequested(Ride ride) {
        kafkaTemplate.send(TOPIC_RIDE_REQUESTED, ride.getId(), ride);
    }

    public void sendRideAccepted(Ride ride) {
        kafkaTemplate.send(TOPIC_RIDE_ACCEPTED, ride.getId(), ride);
    }

    public void sendRideCancelled(Ride ride) {
        kafkaTemplate.send(TOPIC_RIDE_CANCELLED, ride.getId(), ride);
    }

    public void sendRideCompleted(Ride ride) {
        RideCompletedEvent event = RideCompletedEvent.builder()
                .rideId(ride.getId())
                .userId(ride.getUserId())
                .driverId(ride.getDriverId())
                .fare(ride.getFare())
                .rideKm(ride.getRideKm())
                .pickupAddress(ride.getPickupAddress())
                .dropAddress(ride.getDropAddress())
                .completedAt(ride.getCompletedAt())
                .build();
        kafkaTemplate.send(TOPIC_RIDE_COMPLETED, ride.getId(), event);
    }

    public void sendDriverShiftStarted(Object payload, String driverId) {
        kafkaTemplate.send(TOPIC_DRIVER_SHIFT_STARTED, driverId, payload);
    }

    public void sendDriverShiftEnded(Object payload, String driverId) {
        kafkaTemplate.send(TOPIC_DRIVER_SHIFT_ENDED, driverId, payload);
    }

    public void sendDriverLocationUpdated(Object payload, String driverId) {
        kafkaTemplate.send(TOPIC_DRIVER_LOCATION_UPDATED, driverId, payload);
    }
}

