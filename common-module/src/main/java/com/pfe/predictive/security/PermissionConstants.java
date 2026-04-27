package com.pfe.predictive.security;

/**
 * PermissionConstants - Central place for all security permissions and roles.
 *
 * IMPORTANT FIX APPLIED:
 * - Replaced hasAnyRole / hasRole with hasAnyAuthority / hasAuthority
 * - All roles now use ROLE_ prefix to match JWT + SecurityContext
 *
 * This fixes 403 Forbidden issues caused by role mismatch.
 */
public class PermissionConstants {

    // ========================================================================
    // ROLE CONSTANTS
    // ========================================================================
    public static final String ROLE_TECHNICIAN = "ROLE_TECHNICIAN";
    public static final String ROLE_MANAGER = "ROLE_MANAGER";
    public static final String ROLE_STOCK_MANAGER = "ROLE_STOCK_MANAGER";
    public static final String ROLE_DATA_SCIENTIST = "ROLE_DATA_SCIENTIST";
    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final String ROLE_SUPER_ADMIN = "ROLE_SUPER_ADMIN";

    // ========================================================================
    // MAINTENANCE PERMISSIONS
    // ========================================================================
    public static final String PERM_MAINTENANCE_READ =
        "hasAnyAuthority('ROLE_TECHNICIAN','ROLE_MANAGER','ROLE_ADMIN','ROLE_SUPER_ADMIN')";

    public static final String PERM_MAINTENANCE_CREATE =
        "hasAnyAuthority('ROLE_MANAGER','ROLE_ADMIN','ROLE_SUPER_ADMIN')";

    public static final String PERM_MAINTENANCE_UPDATE =
        "hasAnyAuthority('ROLE_TECHNICIAN','ROLE_MANAGER','ROLE_ADMIN','ROLE_SUPER_ADMIN')";

    public static final String PERM_MAINTENANCE_DELETE =
        "hasAnyAuthority('ROLE_ADMIN','ROLE_SUPER_ADMIN')";

    public static final String PERM_MAINTENANCE_APPROVE =
        "hasAnyAuthority('ROLE_MANAGER','ROLE_ADMIN','ROLE_SUPER_ADMIN')";

    // ========================================================================
    // INVENTORY PERMISSIONS
    // ========================================================================
    public static final String PERM_INVENTORY_READ =
        "hasAnyAuthority('ROLE_TECHNICIAN','ROLE_MANAGER','ROLE_STOCK_MANAGER','ROLE_ADMIN','ROLE_SUPER_ADMIN')";

    public static final String PERM_INVENTORY_CREATE =
        "hasAnyAuthority('ROLE_MANAGER','ROLE_STOCK_MANAGER','ROLE_ADMIN','ROLE_SUPER_ADMIN')";

    public static final String PERM_INVENTORY_UPDATE =
        "hasAnyAuthority('ROLE_MANAGER','ROLE_STOCK_MANAGER','ROLE_ADMIN','ROLE_SUPER_ADMIN')";

    public static final String PERM_INVENTORY_DELETE =
        "hasAnyAuthority('ROLE_ADMIN','ROLE_SUPER_ADMIN')";

    public static final String PERM_INVENTORY_REORDER_REQUEST =
        "hasAnyAuthority('ROLE_TECHNICIAN','ROLE_MANAGER','ROLE_STOCK_MANAGER','ROLE_ADMIN','ROLE_SUPER_ADMIN')";

    public static final String PERM_REORDER_APPROVE =
        "hasAnyAuthority('ROLE_MANAGER','ROLE_ADMIN','ROLE_SUPER_ADMIN')";

    public static final String PERM_STOCK_MANAGE =
        "hasAnyAuthority('ROLE_STOCK_MANAGER','ROLE_ADMIN','ROLE_SUPER_ADMIN')";

    // ========================================================================
    // TASK PERMISSIONS
    // ========================================================================
    public static final String PERM_TASK_READ =
        "hasAnyAuthority('ROLE_TECHNICIAN','ROLE_MANAGER','ROLE_ADMIN','ROLE_SUPER_ADMIN')";

    public static final String PERM_TASK_CREATE =
        "hasAnyAuthority('ROLE_MANAGER','ROLE_ADMIN','ROLE_SUPER_ADMIN')";

    public static final String PERM_TASK_UPDATE =
        "hasAnyAuthority('ROLE_TECHNICIAN','ROLE_MANAGER','ROLE_ADMIN','ROLE_SUPER_ADMIN')";

    public static final String PERM_TASK_DELETE =
        "hasAnyAuthority('ROLE_ADMIN','ROLE_SUPER_ADMIN')";

    // ========================================================================
    // ALERT PERMISSIONS
    // ========================================================================
    public static final String PERM_ALERT_READ =
        "hasAnyAuthority('ROLE_TECHNICIAN','ROLE_MANAGER','ROLE_DATA_SCIENTIST','ROLE_ADMIN','ROLE_SUPER_ADMIN')";

    public static final String PERM_ALERT_CREATE =
        "hasAnyAuthority('ROLE_MANAGER','ROLE_ADMIN','ROLE_SUPER_ADMIN')";

    public static final String PERM_ALERT_ACKNOWLEDGE =
        "hasAnyAuthority('ROLE_TECHNICIAN','ROLE_MANAGER','ROLE_ADMIN','ROLE_SUPER_ADMIN')";

    public static final String PERM_ALERT_ESCALATE =
        "hasAnyAuthority('ROLE_MANAGER','ROLE_ADMIN','ROLE_SUPER_ADMIN')";

    public static final String PERM_ALERT_CLOSE =
        "hasAnyAuthority('ROLE_MANAGER','ROLE_ADMIN','ROLE_SUPER_ADMIN')";

    public static final String PERM_ALERT_DELETE =
        "hasAnyAuthority('ROLE_ADMIN','ROLE_SUPER_ADMIN')";

    // ========================================================================
    // MACHINE PERMISSIONS
    // ========================================================================
    public static final String PERM_MACHINE_READ =
        "hasAnyAuthority('ROLE_TECHNICIAN','ROLE_MANAGER','ROLE_DATA_SCIENTIST','ROLE_ADMIN','ROLE_SUPER_ADMIN')";

    public static final String PERM_MACHINE_CREATE =
        "hasAnyAuthority('ROLE_ADMIN','ROLE_SUPER_ADMIN')";

    public static final String PERM_MACHINE_UPDATE =
        "hasAnyAuthority('ROLE_MANAGER','ROLE_ADMIN','ROLE_SUPER_ADMIN')";

    public static final String PERM_MACHINE_DELETE =
        "hasAnyAuthority('ROLE_ADMIN','ROLE_SUPER_ADMIN')";

    // ========================================================================
    // SENSOR DATA PERMISSIONS
    // ========================================================================
    public static final String PERM_SENSORDATA_READ =
        "hasAnyAuthority('ROLE_TECHNICIAN','ROLE_MANAGER','ROLE_DATA_SCIENTIST','ROLE_ADMIN','ROLE_SUPER_ADMIN')";

    public static final String PERM_SENSORDATA_WRITE =
        "hasAnyAuthority('ROLE_DATA_SCIENTIST','ROLE_ADMIN','ROLE_SUPER_ADMIN')";

    // ========================================================================
    // PREDICTION PERMISSIONS
    // ========================================================================
    public static final String PERM_PREDICTION_READ =
        "hasAnyAuthority('ROLE_MANAGER','ROLE_DATA_SCIENTIST','ROLE_ADMIN','ROLE_SUPER_ADMIN')";

    public static final String PERM_PREDICTION_CREATE =
        "hasAnyAuthority('ROLE_DATA_SCIENTIST','ROLE_ADMIN','ROLE_SUPER_ADMIN')";

    public static final String PERM_PREDICTION_MODEL_MANAGE =
        "hasAnyAuthority('ROLE_DATA_SCIENTIST','ROLE_ADMIN','ROLE_SUPER_ADMIN')";

    // ========================================================================
    // USER PERMISSIONS
    // ========================================================================
    public static final String PERM_USER_READ =
        "hasAnyAuthority('ROLE_MANAGER','ROLE_ADMIN','ROLE_SUPER_ADMIN')";

    public static final String PERM_USER_CREATE =
        "hasAnyAuthority('ROLE_ADMIN','ROLE_SUPER_ADMIN')";

    public static final String PERM_USER_UPDATE =
        "hasAnyAuthority('ROLE_ADMIN','ROLE_SUPER_ADMIN')";

    public static final String PERM_USER_DELETE =
        "hasAuthority('ROLE_SUPER_ADMIN')";

    public static final String PERM_USER_ASSIGN_ROLE =
        "hasAuthority('ROLE_SUPER_ADMIN')";

    // ========================================================================
    // DASHBOARD PERMISSIONS
    // ========================================================================
    public static final String PERM_DASHBOARD_READ =
        "hasAnyAuthority('ROLE_TECHNICIAN','ROLE_MANAGER','ROLE_STOCK_MANAGER','ROLE_DATA_SCIENTIST','ROLE_ADMIN','ROLE_SUPER_ADMIN')";

    public static final String PERM_METRICS_READ =
        "hasAnyAuthority('ROLE_MANAGER','ROLE_DATA_SCIENTIST','ROLE_ADMIN','ROLE_SUPER_ADMIN')";

    public static final String PERM_AUDIT_VIEW =
        "hasAnyAuthority('ROLE_ADMIN','ROLE_SUPER_ADMIN')";

    // ========================================================================
    // UTILITY METHODS
    // ========================================================================
    public static boolean isSuperAdminOrAdmin(String role) {
        return ROLE_SUPER_ADMIN.equals(role) || ROLE_ADMIN.equals(role);
    }

    public static boolean isManager(String role) {
        return ROLE_MANAGER.equals(role);
    }

    public static boolean isTechnician(String role) {
        return ROLE_TECHNICIAN.equals(role);
    }

    public static boolean isStockManager(String role) {
        return ROLE_STOCK_MANAGER.equals(role);
    }

    public static boolean isDataScientist(String role) {
        return ROLE_DATA_SCIENTIST.equals(role);
    }

    public static boolean isPrivileged(String role) {
        return isSuperAdminOrAdmin(role) || isManager(role);
    }

    public static boolean canApproveReorders(String role) {
        return isSuperAdminOrAdmin(role) || isManager(role);
    }

    public static boolean canManageStock(String role) {
        return isSuperAdminOrAdmin(role) || isStockManager(role);
    }

    public static boolean canManagePredictions(String role) {
        return isSuperAdminOrAdmin(role) || isDataScientist(role);
    }

    public static boolean canViewMetrics(String role) {
        return isSuperAdminOrAdmin(role) || isManager(role) || isDataScientist(role);
    }

    private PermissionConstants() {
        throw new AssertionError("Cannot instantiate utility class");
    }
}