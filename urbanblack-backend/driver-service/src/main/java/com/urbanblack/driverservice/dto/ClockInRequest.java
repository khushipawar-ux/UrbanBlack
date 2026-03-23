package com.urbanblack.driverservice.dto;

import com.urbanblack.driverservice.entity.FuelLevel;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ClockInRequest {
    private Integer startingOdometer;
    private FuelLevel fuelLevel;
    private LocalDateTime clockInTime;
    private Double latitude;
    private Double longitude;
}