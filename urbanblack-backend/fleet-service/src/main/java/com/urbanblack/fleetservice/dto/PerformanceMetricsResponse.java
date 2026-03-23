package com.urbanblack.fleetservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceMetricsResponse {
    private String period;
    private Integer totalTrips;
    private Integer completedTrips;
    private Integer cancelledTrips;
    private Integer totalDistance;
    private Long totalDuration;
    private Double averageRating;
    private Double totalEarnings;
    private Double fuelEfficiency;
    private Integer punctualityScore;
    private Double customerSatisfaction;
    private Integer safetyScore;
}
