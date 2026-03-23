package com.urbanblack.fleetservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
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

    private String status;   // AVAILABLE, ASSIGNED, IN_USE, MAINTENANCE
}
