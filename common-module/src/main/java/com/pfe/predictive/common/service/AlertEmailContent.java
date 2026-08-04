package com.pfe.predictive.common.service;

import java.time.LocalDateTime;

/**
 * Everything a machine-alert notification email needs to display, populated
 * 1:1 from the persisted Alert row immediately before sending. common-module
 * can't depend on alert-module's Alert entity (dependency runs the other
 * way), so this plain record is how that single source of truth crosses the
 * module boundary without being independently recomputed here.
 */
public record AlertEmailContent(
        Long machineId,
        String machineName,
        String severityLabel,
        String title,
        String message,
        String recommendedAction,
        Double healthScore,
        Double predictedRUL,
        Double anomalyProbability,
        String riskLevel,
        Double failureProbability,
        String anomalyType,
        String predictedFailureType,
        LocalDateTime timestamp
) {
}
