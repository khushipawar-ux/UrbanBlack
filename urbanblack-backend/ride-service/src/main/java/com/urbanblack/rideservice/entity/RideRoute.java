package com.urbanblack.rideservice.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ride_routes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RideRoute {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String rideId;

    @Lob
    private String approachPolyline;

    @Lob
    private String ridePolyline;

    private Double approachKm;
    private Double rideKm;
}

