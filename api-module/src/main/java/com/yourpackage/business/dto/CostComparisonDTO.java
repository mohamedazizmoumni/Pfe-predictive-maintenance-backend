package com.yourpackage.business.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CostComparisonDTO {

    private Long machineId;
    private String machineName;
    private BigDecimal preventiveCost;
    private BigDecimal correctiveCost;
    private BigDecimal estimatedSavings;
    private String recommendation;
    private String urgencyLevel;
}
