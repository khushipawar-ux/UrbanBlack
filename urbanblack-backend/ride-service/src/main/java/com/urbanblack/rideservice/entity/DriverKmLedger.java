package com.urbanblack.rideservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "driver_km_ledger")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverKmLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String driverId;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private Double rideKm;

    @Column(nullable = false)
    private Double deadKm;

    @Column(nullable = false)
    private Double freeRoamingKm;

    @Column(nullable = false)
    private Double totalKm;

    @Column(nullable = false)
    private Double quotaKm;

    @Column(nullable = false)
    private Double overuseKm;

    @Column(nullable = false)
    private Double tomorrowQuota;
}

