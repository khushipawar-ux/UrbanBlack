package com.traffic.management.dto;

import com.urbanblack.common.dto.employee.EmployeeResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepotAllocationSummaryDTO {
    private Long depotId;
    private String depotName;
    private DepotManagerDetailsDTO manager;
    private Integer vehicleCount;
    private Integer driverCount;
    private List<VehicleResponseDTO> vehicles;
    private List<EmployeeResponseDTO> drivers;
}
