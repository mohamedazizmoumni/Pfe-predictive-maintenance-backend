package com.pfe.predictive.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Aggregated dashboard KPIs")
public class DashboardSummaryDTO {

    @Schema(description = "Machine fleet statistics")
    private MachineStats machineStats;

    @Schema(description = "Active alerts summary")
    private AlertStats alertStats;

    @Schema(description = "Maintenance schedule summary")
    private MaintenanceStats maintenanceStats;

    @Schema(description = "Inventory status")
    private InventoryStats inventoryStats;

    @Schema(description = "ML prediction fleet metrics")
    private PredictiveStats predictiveStats;

    @Schema(description = "System health and cache timestamp")
    private SystemHealth systemHealth;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Machine statistics")
    public static class MachineStats {
        @Schema(description = "Total machines", example = "150")
        private Long total;

        @Schema(description = "Operational machines", example = "145")
        private Long operational;

        @Schema(description = "In maintenance", example = "3")
        private Long maintenance;

        @Schema(description = "Faulty machines", example = "2")
        private Long faulty;

        @Schema(description = "Decommissioned", example = "0")
        private Long decommissioned;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Alert statistics")
    public static class AlertStats {
        @Schema(description = "Open (unacknowledged) alerts", example = "8")
        private Long open;

        @Schema(description = "Acknowledged alerts", example = "12")
        private Long acknowledged;

        @Schema(description = "CRITICAL priority count", example = "3")
        private Long critical;

        @Schema(description = "HIGH priority count", example = "5")
        private Long high;

        @Schema(description = "MEDIUM priority count", example = "7")
        private Long medium;

        @Schema(description = "LOW priority count", example = "15")
        private Long low;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Maintenance statistics")
    public static class MaintenanceStats {
        @Schema(description = "Scheduled this week", example = "5")
        private Long scheduledThisWeek;

        @Schema(description = "Overdue maintenance tasks", example = "2")
        private Long overdueCount;

        @Schema(description = "Completed this month", example = "18")
        private Long completedThisMonth;

        @Schema(description = "Average completion time in hours", example = "4.5")
        private Double averageCompletionHours;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Inventory statistics")
    public static class InventoryStats {
        @Schema(description = "Parts below low-stock threshold", example = "7")
        private Long lowStockItems;

        @Schema(description = "Pending reorder requests", example = "3")
        private Long pendingReorderRequests;

        @Schema(description = "Total parts in inventory", example = "450")
        private Long totalParts;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Predictive maintenance statistics")
    public static class PredictiveStats {
        @Schema(description = "Machines with CRITICAL RUL predictions", example = "4")
        private Long machinesWithCriticalRul;

        @Schema(description = "Fleet average RUL in hours", example = "245.7")
        private Double averageFleetRul;

        @Schema(description = "Timestamp of most recent prediction", example = "2026-04-20T14:30:00")
        private LocalDateTime lastPredictionAt;

        @Schema(description = "Total predictions in database", example = "15420")
        private Long totalPredictionRecords;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "System health")
    public static class SystemHealth {
        @Schema(description = "Timestamp when this summary was generated/cached", example = "2026-04-20T14:30:00")
        private LocalDateTime lastUpdated;

        @Schema(description = "Whether data is from cache", example = "true")
        private Boolean fromCache;

        @Schema(description = "Cache TTL in seconds", example = "60")
        private Integer cacheTtlSeconds;
    }
}
