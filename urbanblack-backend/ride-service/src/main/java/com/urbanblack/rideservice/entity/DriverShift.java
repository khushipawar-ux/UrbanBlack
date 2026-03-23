package com.urbanblack.rideservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "driver_shifts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverShift {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String driverId;

    /**
     * The shift UUID from driver-service (used for idempotency checks on Kafka events).
     * Unique index added via migration: idx_driver_shifts_shift_ref
     */
    @Column(name = "shift_ref", unique = true)
    private String shiftRef;

    @Column(nullable = false)
    private LocalDateTime shiftStart;

    private LocalDateTime shiftEnd;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DriverStatus status;

    /** Static goal km for the shift (135 km) – for admin reporting. */
    @Column(name = "goal_km", nullable = false)
    private Double goalKm;

    /** Km driven toward the goal (free roaming km) – for admin reporting. */
    @Column(name = "goal_km_reached", nullable = false)
    private Double goalKmReached;

    @Column(nullable = false)
    private Double totalRideKm;

    @Column(nullable = false)
    private Double totalDeadKm;

    /** Km driven while online without a ride, waiting for requests. */
    @Column(nullable = false)
    private Double totalFreeRoamingKm;

    /** totalDeadKm + totalFreeRoamingKm + totalRideKm */
    @Column(nullable = false)
    private Double totalKm;
}

