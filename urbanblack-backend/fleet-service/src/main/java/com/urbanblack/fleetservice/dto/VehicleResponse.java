package com.urbanblack.fleetservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleResponse {
    private String id;
    private String vehicleNumber;
    private String model;
    private String make;
    private Integer year;
    private String fuelType;
    private Integer capacity;
    private Integer currentKm;
    private LocalDate lastServiceDate;
    private LocalDate nextServiceDate;
    private LocalDate insuranceExpiry;
    private String status;
}
