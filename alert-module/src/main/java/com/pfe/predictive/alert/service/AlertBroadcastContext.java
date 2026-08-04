package com.pfe.predictive.alert.service;

/**
 * ML/telemetry snapshot fed into the /topic/alerts websocket payload and
 * persisted onto the Alert entity's ML-snapshot columns — the same values
 * end up driving the notification email too, so nothing is recomputed
 * differently across the three destinations.
 */
public record AlertBroadcastContext(
        String machineName,
        Double health,
        Double anomalyProbability,
        String riskLevel,
        Double predictedRUL,
        Double failureProbability,
        String anomalyType,
        String predictedFailureType
) {
    public static AlertBroadcastContext empty() {
        return new AlertBroadcastContext(null, null, null, null, null, null, null, null);
    }
}
