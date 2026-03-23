package com.urbanblack.rideservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/**
 * Feign client for user-service lookups.
 * Used to resolve passenger name and phone when dispatching ride offers to drivers.
 */
@FeignClient(name = "user-service")
public interface UserServiceClient {

    @GetMapping("/api/users/{userId}")
    Map<String, Object> getUserById(@PathVariable("userId") String userId);

    @GetMapping("/api/users/admin/analytics/users/total")
    Map<String, Object> getTotalUsers();
}
