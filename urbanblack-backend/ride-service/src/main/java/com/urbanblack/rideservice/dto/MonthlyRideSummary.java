package com.urbanblack.rideservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyRideSummary {
    private String driverId;
    private Long rideCount;
    private Double totalKm;
}
