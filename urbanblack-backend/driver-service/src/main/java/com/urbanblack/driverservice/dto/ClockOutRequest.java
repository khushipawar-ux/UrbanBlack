package com.urbanblack.driverservice.dto;

import com.urbanblack.driverservice.entity.FuelLevel;
import com.urbanblack.driverservice.entity.VehicleCondition;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ClockOutRequest {
    private Integer endingOdometer;
    private FuelLevel fuelLevel;
    private VehicleCondition vehicleCondition;
    private LocalDateTime clockOutTime;
    private Double latitude;
    private Double longitude;
}