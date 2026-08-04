package com.pfe.predictive.alert.service;

import com.pfe.predictive.alert.entity.AlertSeverity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlertDecisionEngineTest {

    private final AlertDecisionEngine engine = new AlertDecisionEngine();

    private AlertDecisionInput input(double health, Double anomalyProbability, String riskLevel,
                                      boolean requiresImmediateAction) {
        return new AlertDecisionInput(
                "Press-14", health, 20.0, anomalyProbability, riskLevel,
                0.1, null, null, requiresImmediateAction);
    }

    @ParameterizedTest
    @CsvSource({
            "30.0, CRITICAL",
            "15.0, CRITICAL",
            "0.0, CRITICAL",
            "40.0, HIGH",
            "31.0, HIGH",
            "50.0, WARNING",
            "41.0, WARNING",
            "50.1, INFO",
            "100.0, INFO",
    })
    void severityFollowsHealthThresholds(double health, AlertSeverity expected) {
        AlertDecision decision = engine.decide(input(health, null, null, false));
        assertEquals(expected, decision.severity());
    }

    @Test
    void requiresImmediateActionForcesCriticalRegardlessOfHealth() {
        AlertDecision decision = engine.decide(input(90.0, null, null, true));
        assertEquals(AlertSeverity.CRITICAL, decision.severity());
    }

    @Test
    void criticalRiskLevelForcesCriticalRegardlessOfHealth() {
        AlertDecision decision = engine.decide(input(90.0, null, "critical", false));
        assertEquals(AlertSeverity.CRITICAL, decision.severity());
    }

    @Test
    void highAnomalyProbabilityEscalatesHealthyMachineToWarning() {
        // health alone (60%) would be INFO, but a >0.8 anomaly probability
        // should still surface a WARNING even though nothing else looks bad.
        AlertDecision decision = engine.decide(input(60.0, 0.85, null, false));
        assertEquals(AlertSeverity.WARNING, decision.severity());
    }

    @Test
    void anomalyProbabilityAtThresholdDoesNotEscalate() {
        AlertDecision decision = engine.decide(input(60.0, 0.8, null, false));
        assertEquals(AlertSeverity.INFO, decision.severity());
    }

    @Test
    void titleAndMessageReflectSeverityAndDetectedIssue() {
        AlertDecisionInput in = new AlertDecisionInput(
                "Press-14", 25.0, 20.0, 0.9, "critical", 0.7,
                "BEARING_FAILURE", "MOTOR_OVERHEAT", false);

        AlertDecision decision = engine.decide(in);

        assertTrue(decision.title().startsWith("CRITICAL — Press-14"));
        assertTrue(decision.message().contains("Bearing Failure"));
        assertTrue(decision.message().contains("Motor Overheat"));
        assertTrue(decision.message().contains("Immediate intervention is recommended."));
        assertEquals("Immediate inspection required. Consider stopping the machine if operating conditions require it.",
                decision.recommendedAction());
    }

    @Test
    void uninformativeAnomalyAndFailureLabelsAreOmittedFromMessage() {
        AlertDecisionInput in = new AlertDecisionInput(
                "Press-14", 25.0, 20.0, null, null, null,
                "NONE", "UNKNOWN", false);

        AlertDecision decision = engine.decide(in);

        assertFalse(decision.message().contains("Detected anomaly"));
        assertFalse(decision.message().contains("Likely failure mode"));
    }

    @Test
    void infoSeverityHasNoClosingRecommendationSentence() {
        AlertDecision decision = engine.decide(input(75.0, null, null, false));
        assertEquals(AlertSeverity.INFO, decision.severity());
        assertTrue(decision.message().contains("No significant issues detected on Press-14."));
        assertEquals("No action required.", decision.recommendedAction());
    }
}
