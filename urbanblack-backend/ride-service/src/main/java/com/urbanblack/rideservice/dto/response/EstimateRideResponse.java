package com.urbanblack.rideservice.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EstimateRideResponse {
    private double rideKm;
    private int durationMin;
    private String pickupAddress;
    private String dropAddress;
    private double fare;
    private String polyline;
    private int nearbyDriversCount;
}
