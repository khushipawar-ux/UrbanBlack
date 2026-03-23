package com.urbanblack.rideservice.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class AdminRideResponseDTO {
    private String id;
    private String rideId; // fallback for frontend
    private String userId;
    private String driverId;
    private String driverName;
    private String driverCity;
    private String depotName;
    private String depotCity;
    private String pickupAddress;
    private String dropAddress;
    private Double pickupLat;
    private Double pickupLng;
    private Double dropLat;
    private Double dropLng;
    private String status;
    private Double rideKm;
    private Double distance; // mapping for frontend
    private Integer durationMin;
    private Integer duration; // mapping for frontend
    private Double fare;
    private LocalDateTime requestedAt;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private String startOtp;
    private String endOtp;
    private String pickup; // alias for frontend
    private String drop; // alias for frontend
}
