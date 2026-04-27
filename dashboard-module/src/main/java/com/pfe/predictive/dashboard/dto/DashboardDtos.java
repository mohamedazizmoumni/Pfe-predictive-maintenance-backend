package com.pfe.predictive.dashboard.dto;

import lombok.*;
import java.util.List;
import java.util.Map;

/**
 * Dashboard Module Response DTOs
 * Provides role-specific dashboard data aggregated from all modules
 *
 * @author Dashboard Module
 * @version 1.0
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {
    private String role;

    private String title;

    private Map<String, Object> sections;

    private List<String> permissions;

    private long timestamp;

    private String message;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSummary {
    private long totalUsers;

    private long activeUsers;

    private long inactiveUsers;

    private long lockedUsers;

    private long adminCount;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MachineSummary {
    private long totalMachines;

    private long operationalMachines;

    private long maintenanceMachines;

    private long faultyMachines;

    private double averageHealth;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertSummary {
    private long totalAlerts;

    private long newAlerts;

    private long acknowledgedAlerts;

    private long escalatedAlerts;

    private long closedAlerts;

    private long criticalAlerts;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceSummary {
    private long scheduledTasks;

    private long inProgressTasks;

    private long completedTasks;

    private long overdueTasks;

    private long approvedTasks;

    private double completionRate;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventorySummary {
    private long totalParts;

    private long lowStockItems;

    private long outOfStockItems;

    private long pendingReorders;

    private double totalInventoryValue;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PredictionSummary {
    private long totalPredictions;

    private long pendingReview;

    private long approved;

    private long actedUpon;

    private long rejected;

    private long activeModels;

    private double averageAccuracy;

    private double averageConfidence;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemHealthSummary {
    private String databaseStatus;

    private String apiStatus;

    private double uptime;

    private double requestsPerSecond;

    private double errorRate;

    private long memoryUsageMB;

    private String lastBackupTime;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamPerformance {
    private int teamSize;

    private int tasksCompleted;

    private double averageCompletionTime;

    private double completionRate;

    private double qualityScore;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KPISummary {
    private double machineAvailability;

    private double maintenanceEfficiency;

    private double meanTimeBetweenFailures;

    private double meanTimeToRepair;

    private double predictiveAccuracy;

    private double costPerMaintenance;
}
