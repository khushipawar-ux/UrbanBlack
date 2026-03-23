package com.urbanblack.driverservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceResponseDTO {
    private String id;
    private String driverId;
    private String status;
    private String availability;
    private LocalDateTime clockInTime;
    private Double clockInLatitude;
    private Double clockInLongitude;
    private LocalDateTime clockOutTime;
    private Double clockOutLatitude;
    private Double clockOutLongitude;
    private LocalDateTime lastOnlineTime;
    private LocalDateTime lastOfflineTime;
    private Long accumulatedActiveSeconds;
    private Long totalActiveMinutes;
    private Integer startingOdometer;
    private Integer endingOdometer;
    private String fuelLevelAtStart;
    private String fuelLevelAtEnd;
    private String vehicleCondition;
}
