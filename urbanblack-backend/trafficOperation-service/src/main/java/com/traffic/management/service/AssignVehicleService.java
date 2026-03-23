package com.traffic.management.service;

import com.traffic.management.dto.AssignVehicleRequest;
import com.traffic.management.entity.AllocationVehicle;
import com.traffic.management.repository.AllocationVehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Service for Assign Vehicles to Depot operations.
 * Table: allocate_vehicles
 */
@Service
@RequiredArgsConstructor
public class AssignVehicleService {

    private final AllocationVehicleRepository allocationVehicleRepository;
    private final com.traffic.management.client.CabRegistrationFeignClient cabRegistrationFeignClient;

    @Transactional
    public AllocationVehicle assignVehicles(AssignVehicleRequest request) {
        // Validate all vehicles exist via Feign
        if (request.getVehicleIds() != null) {
            for (Long vId : request.getVehicleIds()) {
                try {
                    var vehicle = cabRegistrationFeignClient.getVehicleById(vId);
                    if (vehicle == null) {
                        throw new RuntimeException("Vehicle not found in CabRegistration-Service with ID: " + vId);
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Error validating vehicle ID " + vId + ": " + e.getMessage());
                }
            }
        }

        AllocationVehicle record = new AllocationVehicle();
        record.setDepotId(request.getDepotId());
        record.setRegistrationDate(request.getRegistrationDate() != null
                ? request.getRegistrationDate()
                : LocalDate.now());
        record.setVehicleIds(request.getVehicleIds());  // stores comma-separated + sets count
        
        AllocationVehicle savedRecord = allocationVehicleRepository.save(record);

        // Update status of all assigned vehicles to ALLOCATED
        if (request.getVehicleIds() != null) {
            com.traffic.management.dto.UpdateStatusRequest statusRequest = new com.traffic.management.dto.UpdateStatusRequest("ALLOCATED");
            for (Long vId : request.getVehicleIds()) {
                try {
                    cabRegistrationFeignClient.updateVehicleStatus(vId, statusRequest);
                } catch (Exception e) {
                    // Log error but don't fail the whole transaction if status update fails
                    // In a more robust system, you might want to use Kafka or retry logic
                    System.err.println("Failed to update status for vehicle ID " + vId + ": " + e.getMessage());
                }
            }
        }

        return savedRecord;
    }

    public List<AllocationVehicle> getAll() {
        return allocationVehicleRepository.findAll();
    }

    public AllocationVehicle getById(Long id) {
        return allocationVehicleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vehicle assignment not found with id: " + id));
    }

    public List<AllocationVehicle> getByDepot(Long depotId) {
        return allocationVehicleRepository.findByDepotId(depotId);
    }

    @Transactional
    public AllocationVehicle update(Long id, AssignVehicleRequest request) {
        AllocationVehicle record = getById(id);
        if (request.getDepotId() != null)         record.setDepotId(request.getDepotId());
        if (request.getRegistrationDate() != null) record.setRegistrationDate(request.getRegistrationDate());
        if (request.getVehicleIds() != null)      record.setVehicleIds(request.getVehicleIds());
        return allocationVehicleRepository.save(record);
    }

    @Transactional
    public void delete(Long id) {
        allocationVehicleRepository.deleteById(id);
    }
}
