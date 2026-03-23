package com.traffic.management.controller;

import com.traffic.management.dto.DriverDailyAssignmentDTO;
import com.traffic.management.service.DriverDailyAssignmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/assignments")
@RequiredArgsConstructor
public class DriverDailyAssignmentController {

    private final DriverDailyAssignmentService service;

    @PostMapping
    public ResponseEntity<List<DriverDailyAssignmentDTO>> createAssignment(@RequestBody DriverDailyAssignmentDTO dto) {
        log.info("Received request to create assignment for driver: {}, cab: {}", dto.getDriverId(), dto.getCabId());
        return ResponseEntity.ok(service.createAssignment(dto));
    }

    @PostMapping("/bulk")
    public ResponseEntity<List<DriverDailyAssignmentDTO>> createBulkAssignments(@RequestBody com.traffic.management.dto.BulkAssignmentRequest request) {
        log.info("Received request for bulk assignments: {} drivers, {} cabs", 
                request.getDriverIds() != null ? request.getDriverIds().size() : 0,
                request.getCabIds() != null ? request.getCabIds().size() : 0);
        return ResponseEntity.ok(service.createBulkAssignments(request));
    }

    @GetMapping
    public ResponseEntity<List<DriverDailyAssignmentDTO>> getAllAssignments() {
        log.info("Fetching all assignments");
        return ResponseEntity.ok(service.getAllAssignments());
    }

    @GetMapping("/date/{date}")
    public ResponseEntity<List<DriverDailyAssignmentDTO>> getAssignmentsByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        log.info("Fetching assignments for date: {}", date);
        return ResponseEntity.ok(service.getAssignmentsByDate(date));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DriverDailyAssignmentDTO> getAssignmentById(@PathVariable Long id) {
        log.info("Fetching assignment by ID: {}", id);
        return ResponseEntity.ok(service.getAssignmentById(id));
    }

    @GetMapping("/driver/{driverId}")
    public ResponseEntity<List<DriverDailyAssignmentDTO>> getAssignmentsByDriverId(@PathVariable Long driverId) {
        log.info("Fetching all assignments for driverId: {}", driverId);
        return ResponseEntity.ok(service.getAssignmentsByDriverId(driverId));
    }

    /**
     * Primary endpoint for the driver mobile app.
     * Returns today's active (PENDING or ONGOING) assignments for the driver,
     * enriched with shift name/times and depot name/address.
     */
    @GetMapping("/driver/{driverId}/today")
    public ResponseEntity<List<DriverDailyAssignmentDTO>> getTodaysAssignmentsForDriver(@PathVariable Long driverId) {
        log.info("Fetching today's active assignments for driverId: {}", driverId);
        return ResponseEntity.ok(service.getTodaysAssignmentsForDriver(driverId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAssignment(@PathVariable Long id) {
        log.info("Deleting assignment by ID: {}", id);
        service.deleteAssignment(id);
        return ResponseEntity.noContent().build();
    }
}
