package com.traffic.management.controller;

import com.traffic.management.dto.DepotCountReport;
import com.traffic.management.service.ReportingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportingController {

    private final ReportingService reportingService;

    @GetMapping("/depot-wise-vehicle-count")
    public List<DepotCountReport> getDepotWiseVehicleCount() {
        return reportingService.getDepotWiseVehicleCount();
    }

    @GetMapping("/depot-wise-driver-count")
    public List<DepotCountReport> getDepotWiseDriverCount() {
        return reportingService.getDepotWiseDriverCount();
    }

    @GetMapping("/vehicle-utilization")
    public String getVehicleUtilization() {
        return reportingService.getVehicleUtilization();
    }

    @GetMapping("/driver-utilization")
    public String getDriverUtilization() {
        return reportingService.getDriverUtilization();
    }

    @GetMapping("/allocation-summary")
    public String getAllocationSummary() {
        return reportingService.getAllocationSummary();
    }
}
