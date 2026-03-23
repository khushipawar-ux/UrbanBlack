package com.urbanblack.rideservice.dto.request;

import lombok.Data;

@Data
public class AdjustQuotaRequest {
    private String date;
    private double newQuotaKm;
    private String reason;
}

