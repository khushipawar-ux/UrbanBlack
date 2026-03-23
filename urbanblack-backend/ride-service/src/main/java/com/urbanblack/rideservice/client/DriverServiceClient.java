package com.urbanblack.rideservice.client;

import com.urbanblack.rideservice.dto.DriverSummaryDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Feign client for synchronous, authoritative driver state lookups.
 * <p>
 * The service name {@code driver-service} is resolved by Eureka (load-balanced).
 * Used in ride-service when real-time shift status or GPS coordinates are required
 * for ride assignment without waiting for a Kafka event.
 * <p>
 * <b>Do NOT call this client to start/end shifts</b> – shift lifecycle is owned
 * exclusively by driver-service and propagated via Kafka.
 */
@FeignClient(name = "driver-service")
public interface DriverServiceClient {

    /**
     * Fetches a lightweight summary of the driver's shift state and last known location.
     *
     * @param driverId the driver's UUID
     * @return {@link DriverSummaryDto} with shiftId, shiftStatus, isOnline, latitude, longitude
     */
    @GetMapping("/api/driver/profile/summary/{driverId}")
    DriverSummaryDto getDriverSummary(@PathVariable("driverId") String driverId);
}
