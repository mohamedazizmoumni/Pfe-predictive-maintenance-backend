package com.pfe.predictive.common.service;

import java.math.BigDecimal;
import java.util.List;

/**
 * Everything the Priority 7 weekly fleet health digest needs to display.
 * Every field is a real aggregate computed by FleetHealthDigestService from
 * existing repositories/services (ExecutiveSummaryService, AlertQueryService,
 * MaintenanceRepository) — nothing here is fabricated, and fields with no
 * supporting data (e.g. uptime/downtime) are simply not part of this record.
 */
public record FleetHealthDigestEmailContent(
        String periodLabel,                    // pre-formatted, e.g. "Aug 6 - Aug 13, 2026"
        int machineCount,
        Double fleetAverageHealth,              // nullable, 0-100
        long openWorkOrders,
        long overdueWorkOrders,
        BigDecimal budgetUtilizationPercentage, // nullable — null if no budget exists for the current year
        long unresolvedAlerts,
        long newAlertsThisWeek,
        long resolvedAlertsThisWeek,
        Double averageResolutionTimeHours,      // nullable — null if no alert has ever been closed
        long preventiveCompletedThisWeek,
        long correctiveCompletedThisWeek,
        List<TopRisk> topReliabilityRisks,
        String actionUrl
) {
    public record TopRisk(String machineName, Double mtbfHours, Integer failureCount) {
    }
}
