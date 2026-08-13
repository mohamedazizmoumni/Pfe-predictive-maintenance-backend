package com.pfe.predictive.alert.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Aggregated metrics displayed on the alerts dashboard widgets.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertStatsResponse {

    private Long totalAlerts;
    private Long newAlerts;
    private Long acknowledgedAlerts;
    private Long escalatedAlerts;
    private Long closedAlerts;
    private Long criticalCount;
    private Long warningCount;
    private Long infoCount;
    private Long unviewedCount;

    // Real average of (closedDate - createdDate) across CLOSED alerts, computed
    // in AlertQueryService.getAverageResolutionTimeHours(). Null (not 0) when
    // there are no closed alerts yet - 0 would falsely read as "resolved
    // instantly", not "no data".
    private Double averageResolutionTimeHours;
}
