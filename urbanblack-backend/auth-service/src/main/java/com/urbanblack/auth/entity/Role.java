package com.urbanblack.auth.entity;

public enum Role {
    USER,
    DRIVER,
    ADMIN,
    EMPLOYEE,
     /** Manages day-to-day operations of a depot */
    DEPOT_MANAGER,

    /** Monitors and enforces traffic rules */
    TRAFFIC_INSPECTOR,

    /** Allocates cab assignments and tracks fleet */
    FLEET_COORDINATOR,

    /** Handles passenger support and complaint escalations */
    CUSTOMER_SUPPORT,

    /** Handles financial records, billing, and refunds */
    ACCOUNTS_OFFICER,

    /** Manages technical systems and service maintenance */
    IT_ADMINISTRATOR,

    /** Senior officer who oversees zone-level operations */
    ZONE_SUPERVISOR,

    /** Handles hiring, onboarding, and HR functions */
    HR_MANAGER,

    /** Platform-level admin with full access (non-super-admin) */
    OPERATIONS_ADMIN
}
