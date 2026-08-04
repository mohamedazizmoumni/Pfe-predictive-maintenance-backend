package com.pfe.predictive.alert.service;

import com.pfe.predictive.alert.entity.AlertSeverity;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Alert Decision Engine — the single place that turns a machine's current
 * health/ML snapshot into a severity and the text that goes with it.
 * Severity always drives the urgency framing (tone, recommended action);
 * the message additionally weaves in the specific detected anomaly/failure
 * mode from the same snapshot, so two incidents of the same severity don't
 * read as identical form letters.
 */
@Service
public class AlertDecisionEngine {

    private static final Set<String> UNINFORMATIVE_LABELS = Set.of("NONE", "UNKNOWN", "GENERAL_DEGRADATION");

    public AlertDecision decide(AlertDecisionInput input) {
        AlertSeverity severity = determineSeverity(input);
        String title = buildTitle(input.machineName(), severity, input.health());
        String message = buildMessage(input, severity);
        String recommendedAction = buildRecommendedAction(severity);
        return new AlertDecision(severity, title, message, recommendedAction);
    }

    private AlertSeverity determineSeverity(AlertDecisionInput input) {
        double health = input.health();

        if (health <= 30.0
                || input.requiresImmediateAction()
                || "CRITICAL".equalsIgnoreCase(input.riskLevel())) {
            return AlertSeverity.CRITICAL;
        }
        if (health <= 40.0) {
            return AlertSeverity.HIGH;
        }
        if (health <= 50.0
                || (input.anomalyProbability() != null && input.anomalyProbability() > 0.8)) {
            return AlertSeverity.WARNING;
        }
        return AlertSeverity.INFO;
    }

    private String buildTitle(String machineName, AlertSeverity severity, double health) {
        return switch (severity) {
            case CRITICAL -> "CRITICAL — " + machineName + " requires immediate action";
            case HIGH -> "HIGH — " + machineName + " degradation is significant (" + String.format("%.0f%%", health) + ")";
            case WARNING -> "WARNING — " + machineName + " health degrading (" + String.format("%.0f%%", health) + ")";
            case INFO -> machineName + " status update";
        };
    }

    private String buildMessage(AlertDecisionInput input, AlertSeverity severity) {
        String lead = switch (severity) {
            case CRITICAL -> "Critical degradation detected on " + input.machineName() + ".";
            case HIGH -> "Significant degradation detected on " + input.machineName() + ".";
            case WARNING -> "Performance degradation detected on " + input.machineName() + ".";
            case INFO -> "No significant issues detected on " + input.machineName() + ".";
        };
        String detail = describeDetectedIssue(input);
        String closing = switch (severity) {
            case CRITICAL -> "Immediate intervention is recommended.";
            case HIGH -> "Maintenance should be scheduled soon.";
            case WARNING -> "Monitoring and inspection are recommended.";
            case INFO -> "";
        };
        return (lead + " " + detail + " " + closing).replaceAll("\\s+", " ").trim();
    }

    private String describeDetectedIssue(AlertDecisionInput input) {
        StringBuilder detail = new StringBuilder("Health has dropped to " + String.format("%.0f%%", input.health()) + ".");

        if (isInformative(input.anomalyType())) {
            detail.append(" Detected anomaly: ").append(humanize(input.anomalyType())).append(".");
        }
        if (isInformative(input.predictedFailureType())) {
            detail.append(" Likely failure mode: ").append(humanize(input.predictedFailureType())).append(".");
        }
        return detail.toString();
    }

    private boolean isInformative(String label) {
        return label != null && !label.isBlank() && !UNINFORMATIVE_LABELS.contains(label.toUpperCase());
    }

    private String humanize(String enumLikeValue) {
        return Arrays.stream(enumLikeValue.split("_"))
                .filter(word -> !word.isEmpty())
                .map(word -> Character.toUpperCase(word.charAt(0)) + word.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }

    private String buildRecommendedAction(AlertSeverity severity) {
        return switch (severity) {
            case CRITICAL -> "Immediate inspection required. Consider stopping the machine if operating conditions require it.";
            case HIGH -> "Schedule maintenance as soon as possible.";
            case WARNING -> "Continue monitoring. Schedule inspection.";
            case INFO -> "No action required.";
        };
    }
}
