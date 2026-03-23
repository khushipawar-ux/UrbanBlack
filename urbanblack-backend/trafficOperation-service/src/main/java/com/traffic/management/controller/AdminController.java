package com.traffic.management.controller;

import com.traffic.management.dto.DashboardSummary;
import com.traffic.management.entity.DriverDailyAssignment;
import com.traffic.management.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/dashboard/summary")
    public DashboardSummary getDashboardSummary() {
        return adminService.getDashboardSummary();
    }

    @GetMapping("/dashboard/recent-allocations")
    public List<DriverDailyAssignment> getRecentAllocations() {
        return adminService.getRecentAllocations();
    }

    @GetMapping("/drivers")
    public List<com.urbanblack.common.dto.employee.EmployeeResponseDTO> getAllDrivers() {
        return adminService.getAllDrivers();
    }

    @GetMapping("/vehicles")
    public List<com.traffic.management.dto.VehicleResponseDTO> getAllVehicles() {
        return adminService.getAllVehicles();
    }

    @GetMapping("/system/health")
    public String getSystemHealth() {
        return adminService.getSystemHealth();
    }

    @GetMapping("/system/audit-logs")
    public List<String> getAuditLogs() {
        return adminService.getAuditLogs();
    }
}
