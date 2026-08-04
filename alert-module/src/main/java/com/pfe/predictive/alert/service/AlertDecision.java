package com.pfe.predictive.alert.service;

import com.pfe.predictive.alert.entity.AlertSeverity;

/**
 * Output of {@link AlertDecisionEngine}: severity plus the title/message/
 * recommendedAction text that are derived purely from that severity, so
 * they can never contradict each other or duplicate across the email.
 */
public record AlertDecision(
        AlertSeverity severity,
        String title,
        String message,
        String recommendedAction
) {
}
