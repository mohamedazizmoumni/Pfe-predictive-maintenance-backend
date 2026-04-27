package com.pfe.predictive.dashboard.service;

import lombok.Data;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Dashboard Service
 * Aggregates data from all modules and provides role-specific dashboards
 * Each role receives different information based on their permissions
 *
 * @author Dashboard Module
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    /**
     * Determine user's primary role
     */
    public String getUserRole(Authentication auth) {
        if (auth == null || auth.getAuthorities() == null) {
            return "TECHNICIAN"; // default
        }

        // Return highest privilege role
        for (GrantedAuthority authority : auth.getAuthorities()) {
            String role = authority.getAuthority();
            if (role.contains("SUPER_ADMIN")) return "SUPER_ADMIN";
            if (role.contains("ADMIN")) return "ADMIN";
            if (role.contains("MANAGER")) return "MANAGER";
            if (role.contains("DATA_SCIENTIST")) return "DATA_SCIENTIST";
            if (role.contains("STOCK_MANAGER")) return "STOCK_MANAGER";
            if (role.contains("TECHNICIAN")) return "TECHNICIAN";
        }

        return "TECHNICIAN";
    }

    /**
     * Get dashboard data for any role
     * Returns different structure based on user role
     */
    public java.util.Map<String, Object> getDashboardData(String role, Long userId) {
        log.info("Building dashboard for role: {} (UserID: {})", role, userId);

        return switch (role) {
            case "SUPER_ADMIN" -> getSuperAdminDashboard(userId);
            case "ADMIN" -> getAdminDashboard(userId);
            case "MANAGER" -> getManagerDashboard(userId);
            case "DATA_SCIENTIST" -> getDataScientistDashboard(userId);
            case "STOCK_MANAGER" -> getStockManagerDashboard(userId);
            default -> getTechnicianDashboard(userId);
        };
    }

    /**
     * SUPER_ADMIN: Full system view + audit info
     */
    private java.util.Map<String, Object> getSuperAdminDashboard(Long userId) {
        return java.util.Map.ofEntries(
            java.util.Map.entry("role", "SUPER_ADMIN"),
            java.util.Map.entry("title", "System Administration Dashboard"),
            java.util.Map.entry("sections", java.util.Map.ofEntries(
                java.util.Map.entry("systemHealth", java.util.Map.of(
                    "totalMachines", "query_from_machine_service",
                    "totalUsers", "query_from_user_service",
                    "databaseStatus", "connected",
                    "apiStatus", "operational"
                )),
                java.util.Map.entry("auditLog", java.util.Map.of(
                    "recentChanges", "last_100_modifications",
                    "userActivity", "all_users_all_actions",
                    "accessLog", "all_access_attempts"
                )),
                java.util.Map.entry("roleManagement", java.util.Map.of(
                    "totalRoles", 6,
                    "usersPerRole", "distribution_chart",
                    "permissions", "full_permission_matrix"
                )),
                java.util.Map.entry("systemMetrics", java.util.Map.of(
                    "uptime", "application_uptime",
                    "requestsPerSecond", "tps",
                    "errorRate", "percentage"
                ))
            )),
            java.util.Map.entry("permissions", java.util.List.of("READ_ALL", "WRITE_ALL", "DELETE_ALL", "AUDIT_ALL"))
        );
    }

    /**
     * ADMIN: System health + user management
     */
    private java.util.Map<String, Object> getAdminDashboard(Long userId) {
        return java.util.Map.ofEntries(
            java.util.Map.entry("role", "ADMIN"),
            java.util.Map.entry("title", "Administrator Dashboard"),
            java.util.Map.entry("sections", java.util.Map.ofEntries(
                java.util.Map.entry("userManagement", java.util.Map.of(
                    "totalUsers", "query_user_service",
                    "activeUsers", "count_status_active",
                    "recentlyCreated", "last_7_days",
                    "lockedAccounts", "account_locks",
                    "pendingApprovals", "count"
                )),
                java.util.Map.entry("systemOverview", java.util.Map.of(
                    "totalMachines", "query_machine_service",
                    "operationalMachines", "status_operational",
                    "maintenanceDue", "overdue_tasks",
                    "machineAlerts", "status_critical"
                )),
                java.util.Map.entry("roleAssignments", java.util.Map.of(
                    "adminUsers", "count_admin_role",
                    "managerUsers", "count_manager_role",
                    "technicianUsers", "count_technician_role"
                )),
                java.util.Map.entry("alerts", java.util.Map.of(
                    "criticalAlerts", "severity_critical",
                    "unacknowledgedAlerts", "status_new",
                    "escalatedAlerts", "status_escalated"
                ))
            )),
            java.util.Map.entry("permissions", java.util.List.of("USER_MANAGEMENT", "SYSTEM_ADMIN", "ALERT_MGMT"))
        );
    }

    /**
     * MANAGER: Team overview + KPIs
     */
    private java.util.Map<String, Object> getManagerDashboard(Long userId) {
        return java.util.Map.ofEntries(
            java.util.Map.entry("role", "MANAGER"),
            java.util.Map.entry("title", "Manager Dashboard"),
            java.util.Map.entry("sections", java.util.Map.ofEntries(
                java.util.Map.entry("teamOverview", java.util.Map.of(
                    "teamSize", "count_technicians_assigned",
                    "tasksAssigned", "maintenance_count",
                    "tasksCompleted", "maintenance_completed_this_week",
                    "completionRate", "percentage"
                )),
                java.util.Map.entry("machineStatus", java.util.Map.of(
                    "operationalMachines", "status_operational",
                    "maintenanceRequred", "overdue_or_due_soon",
                    "criticalAlerts", "severity_critical",
                    "machineHealth", "average_health_score"
                )),
                java.util.Map.entry("alerts", java.util.Map.of(
                    "byStatus", "count_by_status",
                    "byPriority", "count_by_priority",
                    "pendingApproval", "maintenance_awaiting_sign_off"
                )),
                java.util.Map.entry("kpis", java.util.Map.of(
                    "maintenanceEfficiency", "completed_on_time_percentage",
                    "downtime", "total_hours_this_month"
                ))
            )),
            java.util.Map.entry("permissions", java.util.List.of("TEAM_MGMT", "ALERT_APPROVAL", "MAINTENANCE_APPROVAL"))
        );
    }

    /**
     * DATA_SCIENTIST: ML metrics + prediction trends
     */
    private java.util.Map<String, Object> getDataScientistDashboard(Long userId) {
        return java.util.Map.ofEntries(
            java.util.Map.entry("role", "DATA_SCIENTIST"),
            java.util.Map.entry("title", "Data Science Dashboard"),
            java.util.Map.entry("sections", java.util.Map.ofEntries(
                java.util.Map.entry("modelPerformance", java.util.Map.of(
                    "activeModels", "status_active",
                    "averageAccuracy", "weighted_average",
                    "bestPerformingModel", "highest_f1_score",
                    "recentlyTrained", "last_trained_models"
                )),
                java.util.Map.entry("predictions", java.util.Map.of(
                    "totalPredictions", "this_month",
                    "approvalRate", "approved_rejected_ratio",
                    "averageConfidence", "mean_confidence_score",
                    "riskDistribution", "low_medium_high_critical"
                )),
                java.util.Map.entry("trends", java.util.Map.of(
                    "anomalyTrend", "increasing_decreasing",
                    "failureRiskTrend", "7day_moving_average",
                    "predictionAccuracy", "vs_actual_outcomes"
                )),
                java.util.Map.entry("datasets", java.util.Map.of(
                    "dataQuality", "missing_invalid_percentage",
                    "sensorDataPoints", "total_recorded_month",
                    "anomalousReadings", "count_flagged"
                ))
            )),
            java.util.Map.entry("permissions", java.util.List.of("MODEL_MGMT", "PREDICTION_REVIEW", "DATA_ANALYSIS"))
        );
    }

    /**
     * STOCK_MANAGER: Inventory status + reorders
     */
    private java.util.Map<String, Object> getStockManagerDashboard(Long userId) {
        return java.util.Map.ofEntries(
            java.util.Map.entry("role", "STOCK_MANAGER"),
            java.util.Map.entry("title", "Inventory Management Dashboard"),
            java.util.Map.entry("sections", java.util.Map.ofEntries(
                java.util.Map.entry("inventoryStatus", java.util.Map.of(
                    "totalParts", "count_all_parts",
                    "lowStockItems", "below_reorder_point",
                    "outOfStock", "zero_quantity",
                    "overstock", "above_max_quantity"
                )),
                java.util.Map.entry("reorders", java.util.Map.of(
                    "pendingRequests", "status_requested",
                    "approvedOrders", "status_approved",
                    "inShipment", "status_in_transit",
                    "deliveredThisWeek", "received_count"
                )),
                java.util.Map.entry("costAnalysis", java.util.Map.of(
                    "inventoryValue", "total_cost_all_parts",
                    "monthlyConsumption", "cost_this_month",
                    "reorderCost", "pending_orders_total"
                )),
                java.util.Map.entry("alerts", java.util.Map.of(
                    "urgentReorders", "critical_stock_low",
                    "expiredItems", "past_expiry_date"
                ))
            )),
            java.util.Map.entry("permissions", java.util.List.of("INVENTORY_MGMT", "REORDER_MGMT"))
        );
    }

    /**
     * TECHNICIAN: Assigned tasks + recent alerts
     */
    private java.util.Map<String, Object> getTechnicianDashboard(Long userId) {
        return java.util.Map.ofEntries(
            java.util.Map.entry("role", "TECHNICIAN"),
            java.util.Map.entry("title", "Technician Dashboard"),
            java.util.Map.entry("sections", java.util.Map.ofEntries(
                java.util.Map.entry("myTasks", java.util.Map.of(
                    "assigned", "maintenance_assigned_to_me",
                    "inProgress", "count_in_progress",
                    "completed", "count_completed_this_week",
                    "overdue", "past_scheduled_date"
                )),
                java.util.Map.entry("myAlerts", java.util.Map.of(
                    "assigned", "alerts_for_my_machines",
                    "unacknowledged", "new",
                    "critical", "severity_critical",
                    "recent", "last_24_hours"
                )),
                java.util.Map.entry("machinesMonitored", java.util.Map.of(
                    "totalMachines", "count_assigned",
                    "operationalMachines", "status_ok",
                    "machinesNeedingAttention", "status_alert",
                    "recentlyServiced", "completed_last_7_days"
                )),
                java.util.Map.entry("sensorData", java.util.Map.of(
                    "recentReadings", "sensor_data_last_hour",
                    "anomalies", "readings_flagged",
                    "machineHealth", "average_health_my_machines"
                ))
            )),
            java.util.Map.entry("permissions", java.util.List.of("TASK_EXECUTE", "ALERT_ACKNOWLEDGE", "SENSOR_READ"))
        );
    }
}
