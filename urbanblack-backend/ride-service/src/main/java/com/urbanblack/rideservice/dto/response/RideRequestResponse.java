package com.urbanblack.rideservice.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class RideRequestResponse {
    private String rideId;
    private String status;
    private double rideKm;
    private double fare;
    private int estimatedPickupMin;
    private List<Map<String, Object>> nearbyDrivers;
}
