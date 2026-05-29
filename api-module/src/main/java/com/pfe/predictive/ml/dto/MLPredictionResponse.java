package com.pfe.predictive.ml.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ML Prediction Response DTO
 * 
 * Matches Python ML service PredictionResponseV2 schema.
 * Received from ML service after prediction with explainable AI insights.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MLPredictionResponse {
    
    // Machine Identity
    private Long machineId;
    private String timestamp;
    
    // Core Predictions
    @JsonAlias({"rul"})
    private Double predictedRUL;              // ML-predicted remaining useful life (cycles)
    @JsonAlias({"anomalyScore"})
    private Double anomalyProbability;        // 0.0 - 1.0
    private Double failureProbability;        // 0.0 - 1.0
    private String riskLevel;                 // LOW, MEDIUM, HIGH, CRITICAL
    
    // Classification
    @JsonAlias({"failurePhase"})
    private String predictedFailureType;      // BEARING_FAILURE, THERMAL_FAILURE, etc.
    private String anomalyType;               // ACCELERATED_DEGRADATION, THERMAL_SPIKE, etc.
    
    // Actionable Insights
    private String recommendedAction;         // Action recommendation
    private Boolean requiresImmediateAction;  // Urgent flag
    private Double severity;                  // Overall severity score (0.0 - 1.0)
    private Double confidenceScore;           // Model confidence (0.0 - 1.0)
    
    // Explainability
    private String explanation;
    
    // Metadata
    private String modelVersion;
    private Double processingTimeMs;
}
