package com.urbanblack.driverservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyPerformanceReportDTO {
    private String driverId;
    private String driverName;
    private String empId;
    private String depotName;
    private Long presentDays;
    private Long totalRides;
    private Double totalKm;
    private Double avgRating;
    private Double onlineHours;
    private Double estimatedSalary;
}
