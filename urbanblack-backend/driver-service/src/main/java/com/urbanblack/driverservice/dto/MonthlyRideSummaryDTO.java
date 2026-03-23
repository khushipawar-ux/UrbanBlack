package com.urbanblack.driverservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyRideSummaryDTO {
    private String driverId;
    private Long rideCount;
    private Double totalKm;
}
