package com.pfe.predictive.environment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response shape from ml_service POST /environment/analyze.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnvironmentAnalysisResponse {
    private String riskLevel;
    private List<String> recommendations;
}
