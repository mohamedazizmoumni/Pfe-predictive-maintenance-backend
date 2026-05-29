package com.pfe.predictive.ml.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Batch Prediction Response Wrapper
 * 
 * Matches Python ML service BatchPredictionResponseV2 schema.
 * Contains a list of predictions for multiple machines.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BatchPredictionResponse {
    
    /**
     * List of predictions, one per machine in the request
     */
    private List<MLPredictionResponse> predictions;
    
    /**
     * Total batch processing time in milliseconds
     */
    private Double batchProcessingTimeMs;
}
