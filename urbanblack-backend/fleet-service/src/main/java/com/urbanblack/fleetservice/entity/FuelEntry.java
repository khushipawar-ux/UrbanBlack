package com.urbanblack.fleetservice.entity;

import com.urbanblack.fleetservice.enums.FuelType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "fuel_entry")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FuelEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicle;
    
    private String tripId;

    @Enumerated(EnumType.STRING)
    private FuelType fuelType;
    private Double quantity;
    private Double amount;
    private Integer odometerReading;
    
    private String stationName;
    private String stationAddress;
    private Double latitude;
    private Double longitude;
    
    @Column(columnDefinition = "TEXT")
    private String receiptImage; // Base64 encoded image
    
    private LocalDateTime timestamp;
    
    private String status; // PENDING, APPROVED, REJECTED
    private String driverId;
}
