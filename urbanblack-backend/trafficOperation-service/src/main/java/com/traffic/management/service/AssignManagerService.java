package com.traffic.management.service;

import com.traffic.management.dto.AssignManagerRequest;
import com.traffic.management.entity.AssignManager;
import com.traffic.management.repository.AssignManagerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Service for Assign Manager to Depot operations.
 * Table: assign_manager
 */
@Service
@RequiredArgsConstructor
public class AssignManagerService {

    private final AssignManagerRepository assignManagerRepository;
    private final com.traffic.management.client.EmployeeDetailsFeignClient employeeDetailsFeignClient;

    @Transactional
    public AssignManager assignManager(AssignManagerRequest request) {
        // Validate manager exists via Feign
        try {
            var manager = employeeDetailsFeignClient.getEmployeeById(request.getManagerId());
            if (manager == null) {
                throw new RuntimeException("Manager not found in EmployeeDetails-Service with ID: " + request.getManagerId());
            }
        } catch (Exception e) {
            throw new RuntimeException("Error validating manager: " + e.getMessage());
        }

        AssignManager record = AssignManager.builder()
                .depotId(request.getDepotId())
                .managerId(request.getManagerId())
                .registrationDate(request.getRegistrationDate() != null
                        ? request.getRegistrationDate()
                        : LocalDate.now())
                .build();
        return assignManagerRepository.save(record);
    }

    public List<AssignManager> getAll() {
        return assignManagerRepository.findAll();
    }

    public AssignManager getById(Long id) {
        return assignManagerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("AssignManager record not found with id: " + id));
    }

    public List<AssignManager> getByDepot(Long depotId) {
        return assignManagerRepository.findByDepotId(depotId);
    }

    @Transactional
    public AssignManager update(Long id, AssignManagerRequest request) {
        AssignManager record = getById(id);
        if (request.getManagerId() != null)     record.setManagerId(request.getManagerId());
        if (request.getDepotId() != null)       record.setDepotId(request.getDepotId());
        if (request.getRegistrationDate() != null) record.setRegistrationDate(request.getRegistrationDate());
        return assignManagerRepository.save(record);
    }

    @Transactional
    public void delete(Long id) {
        assignManagerRepository.deleteById(id);
    }
}
