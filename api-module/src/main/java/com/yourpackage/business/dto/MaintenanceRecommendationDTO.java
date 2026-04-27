package com.yourpackage.business.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaintenanceRecommendationDTO {

    private Long machineId;
    private String machineName;
    private String urgencyLevel;
    private String recommendedAction;
    private String justification;
    private BigDecimal estimatedCost;
    private BigDecimal estimatedSavings;
    private boolean partsAvailable;
    private List<String> missingParts;
    private int daysUntilFailure;
    private double failureProbability;
}
