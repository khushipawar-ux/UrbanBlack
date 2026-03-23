package com.urbanblack.rideservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "fare_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FareConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private BigDecimal perKmRate;

    @Column(nullable = false)
    private BigDecimal minFare;
}
