package com.pfe.predictive.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RapportApprovalRequest {

    private boolean approved;

    /** Required when approved is false. */
    private String rejectionReason;

    /**
     * Required when approving a rapport that has one or more failed checklist
     * items — forces the reviewer to explicitly acknowledge why they're
     * approving despite a documented failure, instead of it passing through
     * silently (see MaintenanceRapportService.reviewByManager).
     */
    private String reviewNote;
}
