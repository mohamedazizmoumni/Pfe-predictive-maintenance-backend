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

    public double getAverageResolutionTime() {
        if (totalAlerts == null || totalAlerts == 0) {
            return 0;
        }
        return 1.0;
    }
}
