package com.urbanblack.rideservice.service;

import com.urbanblack.common.dto.RewardEvent;
import com.urbanblack.common.dto.UplineRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RewardEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private static final String QUEUE_NAME = "reward.queue";

    public void publishRewardEvent(Long nodeId, List<UplineRecord> uplines) {
        RewardEvent event = RewardEvent.builder()
                .triggeringNodeId(nodeId)
                .uplines(uplines)
                .totalDeduction(10.0)
                .rewardPerLevel(1.0)
                .build();

        rabbitTemplate.convertAndSend(QUEUE_NAME, event);
    }
}
