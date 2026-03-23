package com.urbanblack.paymentservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "payments",
        indexes = {
                @Index(name = "idx_payment_txn", columnList = "txnId"),
                @Index(name = "idx_payment_ride", columnList = "rideId"),
                @Index(name = "idx_payment_user", columnList = "userId"),
                @Index(name = "idx_payment_driver", columnList = "driver_id"),
                @Index(name = "idx_payment_status", columnList = "status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    /** Gateway transaction id we generate and send (PayU: txnid). */
    @Column(nullable = false, unique = true)
    private String txnId;

    @Column(nullable = false)
    private String rideId;

    @Column(nullable = false)
    private Long userId;

    /** Driver who completed the ride - for admin collection analytics */
    @Column(name = "driver_id")
    private String driverId;

    /** UPI only - all money goes to single PayU account */
    @Column(name = "payment_mode", length = 20)
    private String paymentMode;

    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal walletAmount;

    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal onlineAmount;

    private String paymentGateway;
    private String gatewayTxnId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum Status {
        PENDING, SUCCESS, FAILED
    }
}

