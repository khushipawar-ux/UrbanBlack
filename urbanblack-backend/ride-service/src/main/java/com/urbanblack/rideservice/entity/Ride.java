package com.urbanblack.rideservice.entity;

import jakarta.persistence.*;
import lombok.*;
import com.urbanblack.rideservice.service.NearestDriverService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "rides")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ride {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String userId;

    @Column
    private String driverId;

    @Column(nullable = false)
    private Double pickupLat;

    @Column(nullable = false)
    private Double pickupLng;

    @Column(nullable = false)
    private Double dropLat;

    @Column(nullable = false)
    private Double dropLng;

    private String pickupAddress;
    private String dropAddress;
    private String notes;

    /** Vehicle type requested: economy or premium. Drivers are filtered by matching cab category. */
    @Column
    private String vehicleType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RideStatus status;

    private Double rideKm;
    private Integer durationMin;

    private BigDecimal fare;

    /** Payment split fields (wallet + online). */
    @Column(precision = 12, scale = 2)
    private BigDecimal walletUsed;

    @Column(precision = 12, scale = 2)
    private BigDecimal onlinePaid;

    @Enumerated(EnumType.STRING)
    @Column
    private PaymentStatus paymentStatus;

    private LocalDateTime requestedAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** Optimistic locking — prevents two drivers from accepting the same ride simultaneously. */
    @Version
    private Long version;

    /**
     * Transient list of nearby available drivers populated by {@link com.urbanblack.rideservice.service.RideService}
     * after a successful geo-search. Not persisted to the database.
     */
    @Transient
    private List<NearestDriverService.NearestDriverResult> nearbyDrivers;

    /** Transient – resolved from user-service, included in the JSON response to drivers. */
    @Transient
    private String userName;

    /** Transient – resolved from user-service, included in the JSON response to drivers. */
    @Transient
    private String userPhone;

    /**
     * 4-digit one-time password generated when a driver accepts the ride.
     * The passenger shows this OTP to the driver, who must enter it before starting the trip.
     */
    @Column
    private String otp;

    public enum PaymentStatus {
        PENDING, PAID, FAILED
    }
}
