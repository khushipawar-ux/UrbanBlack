package com.traffic.management.client;

import com.traffic.management.dto.DepotResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(name = "traffic-operations-service", path = "/api/depots")
public interface DepotFeignClient {

    @GetMapping("/all")
    List<DepotResponse> getAllDepots();
}
