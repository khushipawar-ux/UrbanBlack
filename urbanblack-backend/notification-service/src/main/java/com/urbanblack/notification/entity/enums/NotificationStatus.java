package com.urbanblack.notification.entity.enums;

/** Delivery status of the notification */
public enum NotificationStatus {
    /** Notification is pending and will be retried */
    PENDING,

    /** Successfully sent */
    SENT,

    /** Failed after all retry attempts */
    FAILED,

    /** Cancelled before sending */
    CANCELLED
}
