package com.urbanblack.fleetservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.util.Map;

@Data
public class ReturnVehicleRequest {
    @NotBlank(message = "vehicleId is required")
    private String vehicleId;

    @NotNull(message = "endKm is required")
    @Positive(message = "endKm must be greater than 0")
    private Integer endKm;

    @NotNull(message = "endFuel is required")
    @Positive(message = "endFuel must be greater than 0")
    private Integer endFuel;
    private Map<String, String> photos; // front, back, dashboard
    private String damages;
    private String timestamp;
}
