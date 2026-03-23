package com.urbanblack.fleetservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleAssignmentResponse {
    private String vehicleAssignmentId;
    private String vehicleId;
    private String status;
}
