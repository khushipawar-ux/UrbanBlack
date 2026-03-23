package com.urbanblack.fleetservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.util.Map;

@Data
public class TakeVehicleRequest {
    @NotBlank(message = "vehicleId is required")
    private String vehicleId;

    @NotNull(message = "startKm is required")
    @Positive(message = "startKm must be greater than 0")
    private Integer startKm;

    @NotNull(message = "startFuel is required")
    @Positive(message = "startFuel must be greater than 0")
    private Integer startFuel;
    private Map<String, String> photos; // front, back, left, right, dashboard
    private Map<String, Boolean> inspectionChecklist; // exteriorCondition, interiorCleanliness, etc.
    private String timestamp;
}
