package com.pfe.predictive.portal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortalMachineDetailDto {
    private Long machineId;
    private String name;
    private String serialNumber;
    private String location;
    private String model;
    private String statusLabel;
    private Integer healthScore;
    private LocalDateTime installationDate;
    private LocalDateTime lastMaintenanceDate;
    private LocalDateTime nextMaintenanceDate;

    // Prediction, in plain language — never raw risk scores or model internals.
    /** e.g. "Low risk", "Moderate risk", "Elevated risk — service recommended", "Urgent — service required" */
    private String riskSummary;
    /** Plain-language recommended action, when the model has one. */
    private String recommendedAction;
    /** When true, the prediction above is an estimate (ML service was unreachable), not the trained model's output — must be disclosed, not hidden. */
    private boolean predictionEstimated;
    private LocalDateTime predictionUpdatedAt;
}
