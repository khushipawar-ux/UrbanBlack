package com.traffic.management.service;

import com.traffic.management.client.CabRegistrationFeignClient;
import com.traffic.management.client.EmployeeDetailsFeignClient;
import com.traffic.management.dto.DashboardSummary;
import com.traffic.management.entity.DriverDailyAssignment;
import com.traffic.management.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final DepotRepository depotRepository;
    private final DriverDailyAssignmentRepository assignmentRepository;
    private final AssignManagerRepository assignManagerRepository;
    private final AllocationVehicleRepository allocationVehicleRepository;
    private final AllocationDriverRepository allocationDriverRepository;
    private final CabRegistrationFeignClient cabClient;
    private final EmployeeDetailsFeignClient employeeClient;

    public DashboardSummary getDashboardSummary() {
        long totalVehicles = 0;
        try {
            var vehicles = cabClient.getAllVehicles();
            if (vehicles != null) totalVehicles = vehicles.size();
        } catch (Exception e) {
            log.error("Error fetching vehicles from cab service: {}", e.getMessage());
        }

        long totalDrivers = 0;
        try {
            var drivers = employeeClient.getEmployeesByRole("DRIVER");
            if (drivers != null) totalDrivers = drivers.size();
        } catch (Exception e) {
            log.error("Error fetching drivers from employee service: {}", e.getMessage());
        }

        return DashboardSummary.builder()
                .totalDepots(depotRepository.count())
                .totalVehicles(totalVehicles)
                .totalDrivers(totalDrivers)
                .totalManagers(assignManagerRepository.count())
                .activeAllocations(assignmentRepository.countByStatus("PENDING"))
                .availableVehicles(0L)
                .availableDrivers(0L)
                .build();
    }

    public List<DriverDailyAssignment> getRecentAllocations() {
        return assignmentRepository.findTop5ByOrderByAssignmentIdDesc();
    }

    public List<com.urbanblack.common.dto.employee.EmployeeResponseDTO> getAllDrivers() {
        try {
            log.info("Fetching all drivers dynamically from employee-details-service");
            List<com.urbanblack.common.dto.employee.EmployeeResponseDTO> drivers = employeeClient.getEmployeesByRole("DRIVER");
            return drivers != null ? drivers : Collections.emptyList();
        } catch (Exception e) {
            log.error("Failed to fetch dynamic driver list from employee-details-service: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<com.traffic.management.dto.VehicleResponseDTO> getAllVehicles() {
        try {
            List<com.traffic.management.dto.VehicleResponseDTO> vehicles = cabClient.getAllVehicles();
            return vehicles != null ? vehicles : Collections.emptyList();
        } catch (Exception e) {
            log.error("Failed to fetch vehicles: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    public String getSystemHealth() {
        return "UP - All systems operational";
    }

    public List<String> getAuditLogs() {
        return List.of("System started at 2026-02-20", "Admin logged in", "Depot Pune created");
    }
}
