package com.pfe.predictive.ml.dto;

import com.pfe.predictive.core.entity.RiskLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Detailed DTO for PredictionRecord - includes all fields including inputFeaturesSummary.
 * Used for single-record endpoints where full details are needed.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Detailed prediction record with feature data")
public class PredictionRecordDetailDTO {
    
    @Schema(description = "Prediction record ID", example = "1")
    private Long id;

    @Schema(description = "Machine ID", example = "42")
    private Long machineId;

    @Schema(description = "When prediction was generated", example = "2026-04-20T14:30:00")
    private LocalDateTime predictedAt;

    @Schema(description = "Remaining Useful Life in hours", example = "168.5")
    private Double rulValue;

    @Schema(description = "Lower bound confidence interval", example = "150.0", nullable = true)
    private Double confidenceLow;

    @Schema(description = "Upper bound confidence interval", example = "187.0", nullable = true)
    private Double confidenceHigh;

    @Schema(description = "Risk level classification", example = "MEDIUM")
    private RiskLevel riskLevel;

    @Schema(description = "ML model version used", example = "1.2.3")
    private String modelVersion;

    @Schema(description = "User who triggered prediction", example = "technician@example.com")
    private String triggeredBy;

    @Schema(description = "JSON snapshot of input features used", example = "{\"sensor1\": 0.85, \"sensor2\": 0.92, ...}")
    private String inputFeaturesSummary;

    @Schema(description = "Timestamp when record was created", example = "2026-04-20T14:30:00")
    private LocalDateTime createdDate;
}
