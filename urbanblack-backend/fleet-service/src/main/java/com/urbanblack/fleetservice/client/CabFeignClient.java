package com.urbanblack.fleetservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/**
 * Feign client to call cab-registration-service via Eureka.
 * Used to resolve a numeric cab_id → vehicle numberPlate.
 */
@FeignClient(name = "cab-registration-service", path = "/registration")
public interface CabFeignClient {

    @GetMapping("/{id}")
    Map<String, Object> getCabById(@PathVariable("id") Long id);
}
