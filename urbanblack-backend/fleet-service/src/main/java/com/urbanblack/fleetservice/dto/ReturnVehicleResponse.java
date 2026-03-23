package com.urbanblack.fleetservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnVehicleResponse {
    private String vehicleAssignmentId;
    private Integer totalKm;
    private Integer fuelConsumed;
}
