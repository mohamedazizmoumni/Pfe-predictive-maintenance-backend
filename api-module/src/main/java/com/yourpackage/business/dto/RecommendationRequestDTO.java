package com.yourpackage.business.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendationRequestDTO {

    @NotNull(message = "machineId is required")
    private Long machineId;

    @NotNull(message = "failureProbability is required")
    @DecimalMin(value = "0.0", message = "failureProbability must be >= 0.0")
    @DecimalMax(value = "1.0", message = "failureProbability must be <= 1.0")
    private Double failureProbability;

    @NotNull(message = "daysUntilPredictedFailure is required")
    @Min(value = 0, message = "daysUntilPredictedFailure must be >= 0")
    private Integer daysUntilPredictedFailure;

    @NotNull(message = "requiredPartIds is required")
    private List<Long> requiredPartIds;
}
