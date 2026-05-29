package com.pfe.predictive.maintenancecost.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Maintenance Recommendation Response DTO
 * Smart recommendation based on failure probability, cost analysis, and parts availability
 * 
 * @author Finance Module
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaintenanceRecommendationResponse {

    /**
     * Machine ID
     */
    private Long machineId;

    /**
     * Machine name
     */
    private String machineName;

    /**
     * Urgency level: CRITICAL, HIGH, MEDIUM, LOW
     * 
     * CRITICAL: Probability >= 85% OR <= 2 days
     * HIGH: Probability >= 65% OR <= 5 days
     * MEDIUM: Probability >= 40%
     * LOW: Everything else
     */
    private String urgencyLevel;

    /**
     * Recommended action: PREVENTIVE, CORRECTIVE, MONITOR
     * 
     * CRITICAL/HIGH: Always PREVENTIVE
     * MEDIUM: PREVENTIVE if parts available, else CORRECTIVE
     * LOW: MONITOR
     */
    private String recommendedAction;

    /**
     * Detailed justification for the recommendation
     */
    private String justification;

    /**
     * Estimated cost of recommended action
     */
    private BigDecimal estimatedCost;

    /**
     * Estimated savings from preventive action
     */
    private BigDecimal estimatedSavings;

    /**
     * Whether all required parts are in stock
     */
    private boolean partsAvailable;

    /**
     * List of missing part names
     */
    private List<String> missingParts;

    /**
     * Days until predicted failure
     */
    private Integer daysUntilFailure;

    /**
     * Failure probability (0.0 - 1.0)
     */
    private Double failureProbability;
}
