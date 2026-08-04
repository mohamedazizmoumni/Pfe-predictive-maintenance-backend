package com.pfe.predictive.alert.service;

/**
 * Plain telemetry/ML snapshot fed into {@link AlertDecisionEngine}. Kept
 * free of api-module types so alert-module doesn't take on a
 * wrong-direction dependency.
 */
public record AlertDecisionInput(
        String machineName,
        double health,
        Double predictedRUL,
        Double anomalyProbability,
        String riskLevel,
        Double failureProbability,
        String anomalyType,
        String predictedFailureType,
        boolean requiresImmediateAction
) {
}
