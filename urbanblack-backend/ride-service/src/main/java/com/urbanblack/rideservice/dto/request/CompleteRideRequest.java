package com.urbanblack.rideservice.dto.request;

import lombok.Data;

@Data
public class CompleteRideRequest {
    private double actualDropLat;
    private double actualDropLng;
}

