package com.urbanblack.notification.entity;

import com.urbanblack.notification.entity.enums.NotificationStatus;
import com.urbanblack.notification.entity.enums.NotificationType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * NotificationLog — persists every notification attempt.
 * Used for auditing, retry logic and admin dashboard.
 */
@Entity
@Table(name = "notification_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Recipient email address */
    @Column(nullable = false)
    private String recipientEmail;

    /** Recipient name (for display) */
    private String recipientName;

    /** Email subject line */
    @Column(nullable = false)
    private String subject;

    /** Type of notification */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    /** Current delivery status */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationStatus status;

    /** Source service that triggered this notification (e.g. "employee-details-service") */
    private String sourceService;

    /** Reference ID in the source service (e.g. employee ID) */
    private String referenceId;

    /** Error message if failed */
    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    /** Number of send attempts */
    @Column(nullable = false)
    @Builder.Default
    private int retryCount = 0;

    /** When the notification was first queued */
    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /** When the notification was last updated (sent or failed) */
    private LocalDateTime updatedAt;

    @PreUpdate
    public void setLastUpdated() {
        this.updatedAt = LocalDateTime.now();
    }
}
