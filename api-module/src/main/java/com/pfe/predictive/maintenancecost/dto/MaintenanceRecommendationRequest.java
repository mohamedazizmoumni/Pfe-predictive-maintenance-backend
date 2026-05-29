package com.pfe.predictive.maintenancecost.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Maintenance Recommendation Request DTO
 * 
 * @author Finance Module
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaintenanceRecommendationRequest {

    /**
     * Machine ID to analyze
     */
    @NotNull(message = "Machine ID is required")
    private Long machineId;

    /**
     * Failure probability (0.0 - 1.0)
     */
    @NotNull(message = "Failure probability is required")
    @DecimalMin(value = "0.0", message = "Failure probability must be between 0.0 and 1.0")
    @DecimalMax(value = "1.0", message = "Failure probability must be between 0.0 and 1.0")
    private Double failureProbability;

    /**
     * Days until predicted failure
     */
    @NotNull(message = "Days until failure is required")
    @Positive(message = "Days until failure must be positive")
    private Integer daysUntilPredictedFailure;

    /**
     * IDs of required parts (optional)
     */
    private List<Long> requiredPartIds;
}
