package com.urbanblack.driverservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverDetailedSummaryDTO {
    private MonthlyPerformanceReportDTO summary;
    private List<AttendanceResponseDTO> dailyLogs;
}
