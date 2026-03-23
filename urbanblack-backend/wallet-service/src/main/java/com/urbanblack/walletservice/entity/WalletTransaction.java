package com.urbanblack.walletservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "wallet_transactions",
        indexes = {
                @Index(name = "idx_wallet_txn_user_created", columnList = "beneficiary_user,created_at"),
                @Index(name = "idx_wallet_txn_idempotency", columnList = "idempotency_key")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long triggeringNode;
    private Long beneficiaryNode;
    private Long beneficiaryUser;
    private Integer uplineLevel;

    /** External reference (e.g., rideId, payoutId). */
    @Column(name = "reference_id")
    private String referenceId;

    /** Used to prevent double-debit / double-credit on retries. */
    @Column(name = "idempotency_key")
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    private TransactionSource source;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "created_at")
    @Builder.Default
    private java.time.LocalDateTime createdAt = java.time.LocalDateTime.now();

    public enum TransactionType {
        CREDIT, DEBIT
    }

    public enum TransactionSource {
        REWARD, RIDE_PAYMENT, KM_OVERUSE, ADMIN, WITHDRAWAL
    }
}
