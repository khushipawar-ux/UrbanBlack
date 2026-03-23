package com.traffic.management.service;

import com.traffic.management.dto.AssignDriverRequest;
import com.traffic.management.entity.AllocationDriver;
import com.traffic.management.repository.AllocationDriverRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Service for Assign Drivers to Depot operations.
 * Table: assign_driver
 */
@Service
@RequiredArgsConstructor
public class AssignDriverService {

    private final AllocationDriverRepository allocationDriverRepository;
    private final com.traffic.management.client.EmployeeDetailsFeignClient employeeDetailsFeignClient;

    @Transactional
    public AllocationDriver assignDrivers(AssignDriverRequest request) {
        // Validate all drivers exist via Feign
        if (request.getEmployeeIds() != null) {
            for (Long eId : request.getEmployeeIds()) {
                try {
                    var employee = employeeDetailsFeignClient.getEmployeeById(eId);
                    if (employee == null) {
                        throw new RuntimeException("Driver not found in EmployeeDetails-Service with ID: " + eId);
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Error validating driver ID " + eId + ": " + e.getMessage());
                }
            }
        }

        AllocationDriver record = new AllocationDriver();
        record.setDepotId(request.getDepotId());
        record.setRegistrationDate(request.getRegistrationDate() != null
                ? request.getRegistrationDate()
                : LocalDate.now());
        record.setEmployeeIds(request.getEmployeeIds()); // stores comma-separated + sets count
        return allocationDriverRepository.save(record);
    }

    public List<AllocationDriver> getAll() {
        return allocationDriverRepository.findAll();
    }

    public AllocationDriver getById(Long id) {
        return allocationDriverRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Driver assignment not found with id: " + id));
    }

    public List<AllocationDriver> getByDepot(Long depotId) {
        return allocationDriverRepository.findByDepotId(depotId);
    }

    @Transactional
    public AllocationDriver update(Long id, AssignDriverRequest request) {
        AllocationDriver record = getById(id);
        if (request.getDepotId() != null)         record.setDepotId(request.getDepotId());
        if (request.getRegistrationDate() != null) record.setRegistrationDate(request.getRegistrationDate());
        if (request.getEmployeeIds() != null)      record.setEmployeeIds(request.getEmployeeIds());
        return allocationDriverRepository.save(record);
    }

    @Transactional
    public void delete(Long id) {
        allocationDriverRepository.deleteById(id);
    }
}
