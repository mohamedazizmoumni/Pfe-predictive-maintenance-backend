package com.pfe.predictive.common.service;

/**
 * Everything an overdue-maintenance auto-escalation notification needs to
 * display. Replaces MaintenanceEscalationService's previous bare, unbranded
 * inline-HTML div with the same shared Sentinel template used everywhere
 * else in EmailService.
 */
public record EscalationEmailContent(
        Long maintenanceId,
        Long machineId,
        String previousPriority,
        String newPriority,
        String scheduledDate,      // pre-formatted
        int overdueThresholdDays
) {
}
