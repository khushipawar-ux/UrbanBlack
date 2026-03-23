package com.urbanblack.rideservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "driver_km_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverKmLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String driverId;

    @Column(nullable = false)
    private String shiftId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private KmCategory category;

    @Column(nullable = false)
    private Double km;

    private String rideId;

    @Column(nullable = false)
    private LocalDateTime recordedAt;
}

