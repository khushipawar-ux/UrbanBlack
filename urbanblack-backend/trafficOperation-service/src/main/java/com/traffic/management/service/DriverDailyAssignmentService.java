package com.traffic.management.service;

import com.traffic.management.dto.DriverDailyAssignmentDTO;
import java.time.LocalDate;
import java.util.List;

public interface DriverDailyAssignmentService {
    List<DriverDailyAssignmentDTO> createAssignment(DriverDailyAssignmentDTO dto);
    List<DriverDailyAssignmentDTO> createBulkAssignments(com.traffic.management.dto.BulkAssignmentRequest request);
    List<DriverDailyAssignmentDTO> getAllAssignments();
    List<DriverDailyAssignmentDTO> getAssignmentsByDate(LocalDate date);
    DriverDailyAssignmentDTO getAssignmentById(Long id);
    List<DriverDailyAssignmentDTO> getAssignmentsByDriverId(Long driverId);

    /**
     * Returns today's active assignments (status PENDING or ONGOING) for a driver.
     * This is the primary endpoint the driver app calls after login.
     */
    List<DriverDailyAssignmentDTO> getTodaysAssignmentsForDriver(Long driverId);

    void deleteAssignment(Long id);
}
