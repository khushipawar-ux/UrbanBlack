package com.traffic.management.dto;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepotRequest {
    private String depotCode;
    private String depotName;
    private String city;
    private String fullAddress;
    private Double latitude;
    private Double longitude;
    private String zone;
    private Integer capacity;
    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "HH:mm:ss")
    private LocalTime operatingStart;
    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "HH:mm:ss")
    private LocalTime operatingEnd;
    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate registrationDate;
}
