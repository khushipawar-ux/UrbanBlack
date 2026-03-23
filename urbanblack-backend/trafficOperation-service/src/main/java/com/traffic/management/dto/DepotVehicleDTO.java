package com.traffic.management.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepotVehicleDTO {
    private Long depotId;
    private String depotName;
    private List<VehicleResponseDTO> vehicles;
    private Integer vehicleCount;
}
