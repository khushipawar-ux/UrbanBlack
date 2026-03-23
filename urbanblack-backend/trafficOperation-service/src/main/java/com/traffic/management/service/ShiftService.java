package com.traffic.management.service;

import com.traffic.management.dto.ShiftRequest;
import com.traffic.management.entity.ShiftMaster;
import com.traffic.management.repository.ShiftMasterRepository;
import com.traffic.management.client.EmployeeDetailsFeignClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@lombok.extern.slf4j.Slf4j
@Service
@RequiredArgsConstructor
public class ShiftService {

    private final ShiftMasterRepository shiftMasterRepository;
    private final EmployeeDetailsFeignClient employeeFeignClient;

    // --- Shift Master ---

    public ShiftMaster createShift(ShiftRequest request) {
        ShiftMaster shift = ShiftMaster.builder()
                .shiftName(request.getShiftName())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();
        return shiftMasterRepository.save(shift);
    }

    public List<ShiftMaster> getAllShifts() {
        return shiftMasterRepository.findAll();
    }

    public ShiftMaster updateShift(Long id, ShiftRequest request) {
        ShiftMaster shift = shiftMasterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Shift not found: " + id));

        shift.setShiftName(request.getShiftName());
        shift.setStartTime(request.getStartTime());
        shift.setEndTime(request.getEndTime());
        if (request.getIsActive() != null) {
            shift.setIsActive(request.getIsActive());
        }

        return shiftMasterRepository.save(shift);
    }

    public void deleteShift(Long id) {
        shiftMasterRepository.deleteById(id);
    }

    public List<com.urbanblack.common.dto.employee.EmployeeResponseDTO> getAllEmployees() {
        return employeeFeignClient.getAllEmployees();
    }
}
