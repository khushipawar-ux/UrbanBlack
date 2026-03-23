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
public class DepotDriverDTO {
    private Long depotId;
    private String depotName;
    private List<EmployeeResponseDTO> drivers;
    private Integer driverCount;
}
