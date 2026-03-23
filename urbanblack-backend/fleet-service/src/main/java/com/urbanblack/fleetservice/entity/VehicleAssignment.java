package com.urbanblack.fleetservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "vehicle_assignment")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String driverId;
    
    @ManyToOne
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicle;

    private Integer startKm;
    private Integer endKm;
    
    private Integer startFuel;
    private Integer endFuel;

    @Column(columnDefinition = "TEXT")
    private String startPhotos; // JSON string for photos
    
    @Column(columnDefinition = "TEXT")
    private String endPhotos; // JSON string for photos
    
    @Column(columnDefinition = "TEXT")
    private String inspectionChecklist; // JSON string for checklist
    
    @Column(columnDefinition = "TEXT")
    private String damages; // Text description of damages

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    private String status;   // ASSIGNED, IN_USE, COMPLETED
}
