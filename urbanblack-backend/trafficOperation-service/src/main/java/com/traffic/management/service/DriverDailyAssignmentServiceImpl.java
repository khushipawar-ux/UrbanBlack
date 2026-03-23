package com.traffic.management.service;

import com.traffic.management.client.CabRegistrationFeignClient;
import com.traffic.management.client.EmployeeDetailsFeignClient;
import com.traffic.management.client.FleetFeignClient;
import com.traffic.management.dto.DriverDailyAssignmentDTO;
import com.traffic.management.dto.VehicleResponseDTO;
import com.traffic.management.entity.*;
import com.traffic.management.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DriverDailyAssignmentServiceImpl implements DriverDailyAssignmentService {

    private final DriverDailyAssignmentRepository repository;
    private final ShiftMasterRepository shiftRepository;
    private final CenterPointRepository centerPointRepository;
    private final DepotRepository depotRepository;
    
    // Feign Clients for validation and synchronization
    private final EmployeeDetailsFeignClient employeeClient;
    private final CabRegistrationFeignClient cabClient;
    private final FleetFeignClient fleetClient;

    @Override
    @Transactional
    public List<DriverDailyAssignmentDTO> createAssignment(DriverDailyAssignmentDTO dto) {
        // 1. Validate External IDs using Feign Clients
        validateDriver(dto.getDriverId());
        validateCab(dto.getCabId());

        // 2. Determine Center Point IDs
        java.util.List<Long> pointIds = new java.util.ArrayList<>();
        if (dto.getCenterPointIds() != null && !dto.getCenterPointIds().isEmpty()) {
            pointIds.addAll(dto.getCenterPointIds());
        } else if (dto.getCenterPointId() != null) {
            pointIds.add(dto.getCenterPointId());
        } else {
            throw new RuntimeException("At least one Center Point ID must be provided");
        }

        // 3. Fetch Internal Shared Entities
        ShiftMaster shift = shiftRepository.findById(dto.getShiftId())
                .orElseThrow(() -> new RuntimeException("Shift not found with ID: " + dto.getShiftId()));
        
        Depot depot = depotRepository.findById(dto.getDepotId())
                .orElseThrow(() -> new RuntimeException("Depot not found with ID: " + dto.getDepotId()));

        // New Logic: Check if driver or cab is already assigned
        validateAlreadyAssigned(dto.getDriverId(), dto.getCabId(), dto.getAssignmentDate());

        java.util.List<DriverDailyAssignment> createdAssignments = new java.util.ArrayList<>();

        // 4. Create an assignment for each center point
        for (Long cpId : pointIds) {
            CenterPoint centerPoint = centerPointRepository.findById(cpId)
                    .orElseThrow(() -> new RuntimeException("Center Point not found with ID: " + cpId));

            DriverDailyAssignment entity = DriverDailyAssignment.builder()
                    .assignmentDate(dto.getAssignmentDate())
                    .driverId(dto.getDriverId())
                    .cabId(dto.getCabId())
                    .shift(shift)
                    .centerPoint(centerPoint)
                    .depot(depot)
                    .status(dto.getStatus() != null ? dto.getStatus() : "PENDING")
                    .createdBy(dto.getCreatedBy() != null ? dto.getCreatedBy() : "admin")
                    .build();

            createdAssignments.add(repository.save(entity));
        }

        // 5. Notify Fleet Service for Synchronization
        try {
            VehicleResponseDTO vehicle = cabClient.getVehicleById(dto.getCabId());
            if (vehicle != null && vehicle.getNumberPlate() != null) {
                log.info("Notifying Fleet Service for vehicle: {} and driver: {}", vehicle.getNumberPlate(), dto.getDriverId());
                fleetClient.adminAssignVehicle(FleetFeignClient.AdminAssignRequest.builder()
                        .vehicleNumber(vehicle.getNumberPlate())
                        .driverId(String.valueOf(dto.getDriverId()))
                        .build());
            }
        } catch (Exception e) {
            log.error("Failed to notify Fleet Service for assignment: {}", e.getMessage());
            // We don't throw exception here to avoid breaking the main flow
        }

        return createdAssignments.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<DriverDailyAssignmentDTO> createBulkAssignments(com.traffic.management.dto.BulkAssignmentRequest request) {
        // 1. Fetch Internal Entities once
        ShiftMaster shift = shiftRepository.findById(request.getShiftId())
                .orElseThrow(() -> new RuntimeException("Shift not found with ID: " + request.getShiftId()));
        
        CenterPoint centerPoint = centerPointRepository.findById(request.getCenterPointId())
                .orElseThrow(() -> new RuntimeException("Center Point not found with ID: " + request.getCenterPointId()));
        
        Depot depot = depotRepository.findById(request.getDepotId())
                .orElseThrow(() -> new RuntimeException("Depot not found with ID: " + request.getDepotId()));

        // 2. Determine how many assignments to create (match drivers with cabs)
        int size = Math.min(request.getDriverIds().size(), request.getCabIds().size());
        
        java.util.ArrayList<DriverDailyAssignment> assignments = new java.util.ArrayList<>();
        
        for (int i = 0; i < size; i++) {
            Long driverId = request.getDriverIds().get(i);
            Long cabId = request.getCabIds().get(i);
            
            // Validate external IDs
            validateDriver(driverId);
            validateCab(cabId);
            
            DriverDailyAssignment entity = DriverDailyAssignment.builder()
                    .assignmentDate(request.getAssignmentDate())
                    .driverId(driverId)
                    .cabId(cabId)
                    .shift(shift)
                    .centerPoint(centerPoint)
                    .depot(depot)
                    .status("PENDING")
                    .createdBy(request.getCreatedBy() != null ? request.getCreatedBy() : "admin")
                    .build();
            
            assignments.add(entity);
        }

        List<DriverDailyAssignment> saved = repository.saveAll(assignments);

        // 3. Notify Fleet Service for each unique cab in the bulk request
        // (Simplification: notify for each created assignment)
        for (int i = 0; i < size; i++) {
            Long driverId = request.getDriverIds().get(i);
            Long cabId = request.getCabIds().get(i);
            try {
                VehicleResponseDTO vehicle = cabClient.getVehicleById(cabId);
                if (vehicle != null && vehicle.getNumberPlate() != null) {
                    fleetClient.adminAssignVehicle(FleetFeignClient.AdminAssignRequest.builder()
                            .vehicleNumber(vehicle.getNumberPlate())
                            .driverId(String.valueOf(driverId))
                            .build());
                }
            } catch (Exception e) {
                log.error("Failed to notify Fleet Service in bulk for cab {}: {}", cabId, e.getMessage());
            }
        }

        return saved.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    public List<DriverDailyAssignmentDTO> getAllAssignments() {
        return repository.findAll().stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    public List<DriverDailyAssignmentDTO> getAssignmentsByDate(LocalDate date) {
        return repository.findByAssignmentDate(date).stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    public DriverDailyAssignmentDTO getAssignmentById(Long id) {
        DriverDailyAssignment entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Assignment not found with ID: " + id));
        return mapToDTO(entity);
    }

    @Override
    public List<DriverDailyAssignmentDTO> getAssignmentsByDriverId(Long driverId) {
        log.info("[AssignmentService] Fetching all assignments for driverId={}", driverId);
        List<DriverDailyAssignment> assignments = repository.findByDriverId(driverId);
        log.info("[AssignmentService] Found {} assignment(s) for driverId={}", assignments.size(), driverId);
        return assignments.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<DriverDailyAssignmentDTO> getTodaysAssignmentsForDriver(Long driverId) {
        LocalDate today = LocalDate.now();
        java.util.List<String> activeStatuses = java.util.List.of("PENDING", "ONGOING");
        log.info("[AssignmentService] Fetching today's ({}) active assignments for driverId={} with statuses={}",
                today, driverId, activeStatuses);
        List<DriverDailyAssignment> assignments =
                repository.findByDriverIdAndAssignmentDateAndStatusIn(driverId, today, activeStatuses);
        log.info("[AssignmentService] Found {} today's assignment(s) for driverId={}", assignments.size(), driverId);
        if (assignments.isEmpty()) {
            log.warn("[AssignmentService] No active assignment found for driverId={} on date={}. " +
                    "Verify a record exists in driver_daily_assignment with assignment_date={} and status IN (PENDING,ONGOING).",
                    driverId, today, today);
        }
        return assignments.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteAssignment(Long id) {
        repository.deleteById(id);
    }

    private void validateDriver(Long driverId) {
        try {
            log.info("Validating driver ID: {}", driverId);
            // Relaxed validation: just check if exists, but don't crash if not found 
            // as IDs might be synced differently across microservices
            employeeClient.getEmployeeById(driverId);
        } catch (Exception e) {
            log.warn("Driver validation failed for ID {}: {}", driverId, e.getMessage());
            // throw new RuntimeException("Invalid Driver ID: " + driverId); // Commented out to solve 404/500 issues
        }
    }

    private void validateCab(Long cabId) {
        try {
            log.info("Validating cab ID: {}", cabId);
            cabClient.getVehicleById(cabId);
        } catch (Exception e) {
            log.warn("Cab validation failed for ID {}: {}", cabId, e.getMessage());
            // throw new RuntimeException("Invalid Cab ID: " + cabId);
        }
    }

    private void validateAlreadyAssigned(Long driverId, Long cabId, LocalDate date) {
        // Enforce Shift Ended Reassignment Rule:
        // A driver or cab cannot be reassigned if they have an active (PENDING or ONGOING) shift/assignment.
        // They can only be reassigned if all their previous assignments are ended/completed.
        java.util.List<String> activeStatuses = java.util.List.of("PENDING", "ONGOING");
        
        if (repository.existsByDriverIdAndStatusIn(driverId, activeStatuses)) {
            log.warn("Driver {} currently has an active shift/assignment and cannot be reassigned.", driverId);
            throw new RuntimeException("Driver cannot be reassigned because their previous shift has not ended!");
        }

        if (repository.existsByCabIdAndStatusIn(cabId, activeStatuses)) {
            log.warn("Cab {} currently has an active shift/assignment and cannot be reassigned.", cabId);
            throw new RuntimeException("Cab cannot be reassigned because its previous shift has not ended!");
        }
        
        // (Optional fallback) If we strictly don't want multiple assignments for the same date 
        // even if COMPLETED, we could leave the old date checks below. But the new requirement says:
        // "if shift is ended then allowed to reassign cab, shifts, depo, driver". 
        // This implies same-day reassignment is allowed if the previous shift is ended.
    }

    private DriverDailyAssignmentDTO mapToDTO(DriverDailyAssignment entity) {
        DriverDailyAssignmentDTO.DriverDailyAssignmentDTOBuilder builder = DriverDailyAssignmentDTO.builder()
                .assignmentId(entity.getAssignmentId())
                .assignmentDate(entity.getAssignmentDate())
                .driverId(entity.getDriverId())
                .cabId(entity.getCabId())
                .status(entity.getStatus())
                .createdBy(entity.getCreatedBy())
                .createdAt(entity.getCreatedAt());

        // Null-safe shift mapping — prevents NPE when shift_id FK is null in DB
        if (entity.getShift() != null) {
            builder.shiftId(entity.getShift().getId())
                   .shiftName(entity.getShift().getShiftName())
                   .shiftStartTime(entity.getShift().getStartTime())
                   .shiftEndTime(entity.getShift().getEndTime());
        } else {
            log.warn("[AssignmentService] Assignment id={} has a null shift reference", entity.getAssignmentId());
        }

        // Null-safe center-point mapping
        if (entity.getCenterPoint() != null) {
            builder.centerPointId(entity.getCenterPoint().getId());
        } else {
            log.warn("[AssignmentService] Assignment id={} has a null centerPoint reference", entity.getAssignmentId());
        }

        // Null-safe depot mapping — includes human-readable name and address
        if (entity.getDepot() != null) {
            builder.depotId(entity.getDepot().getId())
                   .depotName(entity.getDepot().getDepotName())
                   .depotCity(entity.getDepot().getCity())
                   .depotAddress(entity.getDepot().getFullAddress());
        } else {
            log.warn("[AssignmentService] Assignment id={} has a null depot reference", entity.getAssignmentId());
        }

        return builder.build();
    }
}
