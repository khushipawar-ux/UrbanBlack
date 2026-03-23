package com.traffic.management.service;

import com.traffic.management.dto.DepotCountReport;
import com.traffic.management.repository.DriverDailyAssignmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportingService {

    private final DriverDailyAssignmentRepository assignmentRepository;

    public List<DepotCountReport> getDepotWiseVehicleCount() {
        return assignmentRepository.getDepotWiseAssignmentCount();
    }

    public List<DepotCountReport> getDepotWiseDriverCount() {
        return assignmentRepository.getDepotWiseAssignmentCount();
    }

    public String getVehicleUtilization() {
        return "85% utilization across all depots";
    }

    public String getDriverUtilization() {
        return "92% active duty rate";
    }

    public String getAllocationSummary() {
        return "Total daily assignments: " + assignmentRepository.count() + ". Pending: " + assignmentRepository.countByStatus("PENDING");
    }
}
