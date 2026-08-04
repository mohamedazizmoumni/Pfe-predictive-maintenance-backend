package com.pfe.predictive.explainability.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PredictionExplanation {
    private Long machineId;
    private String riskLevel;
    private Double failureProbability;
    private Double confidenceScore;
    private LocalDateTime predictedAt;
    private String summary;
    private List<PredictionFactor> topFactors;
    private boolean hasSensorData;
}
