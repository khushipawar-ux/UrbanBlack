package com.urbanblack.driverservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@Table(name = "shifts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Shift {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String driverId;

    @Enumerated(EnumType.STRING)
    private ShiftStatus status;

    @Enumerated(EnumType.STRING)
    private DriverAvailability availability;

    private LocalDateTime clockInTime;
    @Column(name = "clock_in_latitude")
    private Double clockInLatitude;
    @Column(name = "clock_in_longitude")
    private Double clockInLongitude;

    private LocalDateTime clockOutTime;
    @Column(name = "clock_out_latitude")
    private Double clockOutLatitude;
    @Column(name = "clock_out_longitude")
    private Double clockOutLongitude;

    // ── Duration tracking (timezone-agnostic) ────────────────────────────
    /**
     * UTC epoch second when driver last went ONLINE.
     * NOT stored in the database – derived from lastOnlineTime on the fly.
     */
    @Transient
    private Long lastOnlineEpochSecond;

    /**
     * Returns the epoch second from lastOnlineTime when the transient field is
     * null.
     * This bridges the gap after a JPA reload (transient fields are not hydrated).
     */
    public Long getLastOnlineEpochSecond() {
        if (lastOnlineEpochSecond != null)
            return lastOnlineEpochSecond;
        if (lastOnlineTime != null && availability == DriverAvailability.ONLINE) {
            return lastOnlineTime.atZone(ZoneId.of("Asia/Kolkata")).toInstant().getEpochSecond();
        }
        return null;
    }

    /**
     * Human-readable IST timestamp when driver last went ONLINE.
     */
    @Column(name = "last_online_time")
    private LocalDateTime lastOnlineTime;

    /**
     * Human-readable IST timestamp when driver last went OFFLINE.
     */
    @Column(name = "last_offline_time")
    private LocalDateTime lastOfflineTime;

    /** Total online seconds accumulated so far */
    private long accumulatedActiveSeconds;

    /** Computed at clock-out: accumulatedActiveSeconds / 60. */
    private long totalActiveMinutes;

    // ── Vehicle handover fields ──────────────────────────────────────────
    private Integer startingOdometer;

    @Enumerated(EnumType.STRING)
    private FuelLevel fuelLevelAtStart;

    private Integer endingOdometer;

    @Enumerated(EnumType.STRING)
    private FuelLevel fuelLevelAtEnd;

    @Enumerated(EnumType.STRING)
    private VehicleCondition vehicleCondition;
}