package com.traffic.management.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@FeignClient(name = "fleet-service", path = "/fleet/api/vehicle")
public interface FleetFeignClient {

    @PostMapping("/admin/assign")
    void adminAssignVehicle(@RequestBody AdminAssignRequest request);

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class AdminAssignRequest {
        private String vehicleNumber;
        private String driverId;
    }
}
