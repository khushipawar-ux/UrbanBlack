package com.urbanblack.rideservice.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class RideStatusResponse {
    private String rideId;
    private String status;
    private Map<String, Object> driver;
}

