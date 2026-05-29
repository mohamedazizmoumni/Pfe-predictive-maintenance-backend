package com.pfe.predictive.maintenancecost.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Cost Comparison Response DTO
 * Compares preventive vs corrective maintenance costs
 * 
 * @author Finance Module
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CostComparisonResponse {

    /**
     * Machine ID
     */
    private Long machineId;

    /**
     * Machine name
     */
    private String machineName;

    /**
     * Cost of preventive maintenance
     */
    private BigDecimal preventiveCost;

    /**
     * Cost of corrective (failure) maintenance
     * Includes production loss + emergency labor + emergency parts
     */
    private BigDecimal correctiveCost;

    /**
     * Estimated savings from preventive action
     * Formula: correctiveCost - preventiveCost
     */
    private BigDecimal estimatedSavings;

    /**
     * Recommendation text
     * Examples:
     * - "Strongly recommended: preventive action saves significant cost"
     * - "Preventive action is more cost-effective"
     * - "Costs are comparable - monitor closely"
     */
    private String recommendation;

    /**
     * Urgency level: CRITICAL, HIGH, MEDIUM, LOW
     */
    private String urgencyLevel;

    /**
     * Estimated downtime hours if failure occurs
     */
    private Double estimatedDowntimeHours;
}
