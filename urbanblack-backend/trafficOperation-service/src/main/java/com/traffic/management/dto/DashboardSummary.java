package com.traffic.management.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardSummary {
    private long totalDepots;
    private long totalVehicles;
    private long totalDrivers;
    private long totalManagers;
    private long activeAllocations;
    private long availableVehicles;
    private long availableDrivers;
}
