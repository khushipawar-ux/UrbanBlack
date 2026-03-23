package com.urbanblack.rideservice.kafka;

import com.urbanblack.common.event.RidePaymentCompletedEvent;
import com.urbanblack.rideservice.service.RewardTreeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RideCompletionConsumer {

    private final RewardTreeService rewardTreeService;

    @KafkaListener(
            topics = "ride.payment.completed",
            groupId = "urbanblack-reward-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onRidePaymentCompleted(RidePaymentCompletedEvent event) {
        log.info("[Kafka] Received ride.payment.completed | rideId={} userId={}",
                event.getRideId(), event.getUserId());

        try {
            Long numericUserId = Long.parseLong(event.getUserId());
            rewardTreeService.insertNode(numericUserId);
            log.info("[Kafka] Reward node generated for userId={} (rideId={})", numericUserId, event.getRideId());
        } catch (NumberFormatException e) {
            log.error("[Kafka] Non-numeric userId in event rideId={}: {}", event.getRideId(), event.getUserId());
        } catch (Exception e) {
            log.error("[Kafka] Error generating reward node for ride {}: {}", event.getRideId(), e.getMessage(), e);
        }
    }
}
