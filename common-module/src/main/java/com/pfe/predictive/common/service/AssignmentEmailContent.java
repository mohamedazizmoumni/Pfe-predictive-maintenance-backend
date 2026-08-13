package com.pfe.predictive.common.service;

/**
 * Everything a "you've been assigned work" notification email needs to
 * display — shared by task-module's TaskService and api-module's
 * MaintenanceController so both send the same modern, Sentinel-branded
 * template instead of each hand-rolling their own (one used to be plain
 * text, the other a bespoke inline-HTML block with different branding).
 */
public record AssignmentEmailContent(
        String recipientName,
        String entityTypeLabel,   // "Task" or "Maintenance Work Order"
        Long entityId,
        String title,
        String description,       // nullable
        String priority,
        String status,
        Long machineId,            // nullable
        String dateLabel,          // e.g. "Due Date" / "Scheduled Date"
        String dateValue,          // pre-formatted, nullable
        String assignedBy,         // nullable
        String actionUrl,
        String actionLabel         // e.g. "Open Task" / "View Work Order"
) {
}
