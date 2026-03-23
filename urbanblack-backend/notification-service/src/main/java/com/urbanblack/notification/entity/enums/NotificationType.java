package com.urbanblack.notification.entity.enums;

/**
 * Notification type — used for filtering logs and routing templates.
 */
public enum NotificationType {
    /** Employee onboarding credentials email */
    EMPLOYEE_CREDENTIALS,

    /** OTP via email (fallback when SMS not configured) */
    OTP_EMAIL,

    /** Welcome email for new users */
    WELCOME,

    /** Password reset link */
    PASSWORD_RESET,

    /** General system alert to admin/operations */
    SYSTEM_ALERT,

    /** Role change or permission update */
    ROLE_UPDATE,

    /** Account suspended or blocked */
    ACCOUNT_STATUS_CHANGE,

    /** Custom / ad-hoc notification sent from admin panel */
    CUSTOM
}
