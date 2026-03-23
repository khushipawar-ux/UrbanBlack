package com.urbanblack.rideservice.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReceiptResponse {
    private String rideId;
    private String pickup;
    private String drop;
    private double distanceKm;
    private double fare;
    private String ridePolyline;
    private String date;
}
