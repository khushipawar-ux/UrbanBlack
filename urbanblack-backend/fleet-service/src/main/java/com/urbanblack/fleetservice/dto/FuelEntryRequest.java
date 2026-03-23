package com.urbanblack.fleetservice.dto;

import com.urbanblack.fleetservice.enums.FuelType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class FuelEntryRequest {
    @NotBlank(message = "vehicleId is required")
    private String vehicleId;
    private String tripId;

    @NotNull(message = "fuelType is required")
    private FuelType fuelType;

    @NotNull(message = "quantity is required")
    @Positive(message = "quantity must be greater than 0")
    private Double quantity;

    @NotNull(message = "amount is required")
    @Positive(message = "amount must be greater than 0")
    private Double amount;

    @NotNull(message = "odometerReading is required")
    @Positive(message = "odometerReading must be greater than 0")
    private Integer odometerReading;

    @NotBlank(message = "stationName is required")
    private String stationName;
    private String stationAddress;
    private Double latitude;
    private Double longitude;
    private String receiptImage; // Base64 encoded
    private String timestamp;
}
