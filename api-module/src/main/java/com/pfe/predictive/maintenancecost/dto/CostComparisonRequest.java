package com.pfe.predictive.maintenancecost.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Cost Comparison Request DTO
 * 
 * @author Finance Module
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CostComparisonRequest {

    /**
     * Machine ID to analyze
     */
    @NotNull(message = "Machine ID is required")
    private Long machineId;

    /**
     * Maintenance action ID to compare
     */
    @NotNull(message = "Action ID is required")
    private Long actionId;

    /**
     * Estimated downtime hours if failure occurs
     */
    @NotNull(message = "Estimated downtime hours is required")
    @Positive(message = "Downtime hours must be positive")
    private Double estimatedFailureDowntimeHours;
}
