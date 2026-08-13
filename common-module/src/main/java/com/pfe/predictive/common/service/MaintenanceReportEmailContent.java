package com.pfe.predictive.common.service;

/**
 * Everything a "maintenance report" notification needs — shared by the
 * internal (manager, full detail + PDF) and customer-facing (sanitized, no
 * PDF) email variants sent from MaintenanceRapportService on manager
 * approval. Fields marked internal-only must never be rendered into the
 * customer-facing template.
 */
public record MaintenanceReportEmailContent(
        Long rapportId,
        String machineLabel,
        Long machineId,
        String workPerformed,       // internal only
        String technicianName,      // internal only
        String approvedBy,          // internal only
        String approvedDateLabel,   // pre-formatted, internal only
        String totalCostLabel,      // pre-formatted, internal only
        String actionUrl
) {
}
