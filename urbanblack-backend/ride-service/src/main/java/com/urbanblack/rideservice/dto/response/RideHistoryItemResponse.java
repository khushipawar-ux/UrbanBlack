package com.urbanblack.rideservice.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RideHistoryItemResponse {
    private String rideId;
    private String date;
    private String pickup;
    private String drop;
    private String status;
    private double rideKm;
    private double fare;
    private int durationMin;
    private String driverName;
}

