package com.traffic.management.dto;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonFormat;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverDailyAssignmentDTO {
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long assignmentId;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate assignmentDate;

    private Long driverId;
    private Long cabId;

    // Shift fields
    private Long shiftId;
    private String shiftName;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ss")
    private LocalTime shiftStartTime;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ss")
    private LocalTime shiftEndTime;

    // Center point / Depot fields
    private Long centerPointId;
    private java.util.List<Long> centerPointIds;
    private Long depotId;
    private String depotName;
    private String depotCity;
    private String depotAddress;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String status;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String createdBy;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
}
