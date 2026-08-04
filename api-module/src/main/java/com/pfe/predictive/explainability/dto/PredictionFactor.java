package com.pfe.predictive.explainability.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PredictionFactor {
    private String sensorName;
    private String sensorType;
    private Double currentValue;
    private Double normalMin;
    private Double normalMax;
    private String unit;
    /** How far outside the normal range, as a percentage of the range width; 0 if within range. */
    private double deviationPercent;
    /** "high" / "medium" / "low" — deviationPercent bucketed for display. */
    private String contribution;
}
