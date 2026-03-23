package EmployeeDetails_Service.EmployeeDetails_Service.Entity.enums;

/**
 * Urban Black – Employee Role Enum
 *
 * 10 roles used for employee login / access control.
 * These roles are assigned when an employee is onboarded
 * and are sent along with login credentials via email.
 */
public enum EmployeeRole {

    /** Operates vehicles on assigned routes */
    DRIVER,

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
