package com.urban.cabregisterationservice.service;

import com.urban.cabregisterationservice.entity.CabApplication;
import com.urban.cabregisterationservice.repository.CabApplicationRepository;
import com.urbanblack.common.dto.CabStatusUpdateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class CabStatusUpdateConsumer {
    private final CabApplicationRepository cabApplicationRepository;
    @KafkaListener(topics = "cab-status-updates", groupId = "cab-registration-service-group")
    public void updateStatus(CabStatusUpdateEvent event){
        CabApplication application =
                cabApplicationRepository.findById(event.getCabApplicationId())
                        .orElseThrow(() ->
                                new RuntimeException("Cab application not found")
                        );

        application.setStatus(event.getStatus());
        cabApplicationRepository.save(application);

        log.info("✅ Status updated to {} for cabApplicationId={}",
                event.getStatus(),
                event.getCabApplicationId());
    }
}
