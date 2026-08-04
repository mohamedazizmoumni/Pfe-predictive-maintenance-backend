package com.pfe.predictive.explainability.service;

import com.pfe.predictive.core.entity.PredictionHistory;
import com.pfe.predictive.core.entity.Sensor;
import com.pfe.predictive.data.repository.PredictionHistoryRepository;
import com.pfe.predictive.data.repository.SensorRepository;
import com.pfe.predictive.explainability.dto.PredictionExplanation;
import com.pfe.predictive.explainability.dto.PredictionFactor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/**
 * Builds a "why" breakdown for a machine's latest prediction by comparing
 * each sensor's last reading against its normal range (minRange/maxRange).
 * Deliberately rule-based, not a real SHAP/feature-importance readout from
 * the ML model — the model doesn't expose per-feature attribution today, so
 * this is the honest, explainable substitute: "which sensors are furthest
 * outside normal, and by how much."
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PredictionExplanationService {

    private final PredictionHistoryRepository predictionHistoryRepository;
    private final SensorRepository sensorRepository;

    public PredictionExplanation explain(Long machineId) {
        PredictionHistory latest = predictionHistoryRepository
                .findFirstByMachineIdOrderByTimestampDesc(machineId)
                .orElse(null);

        List<Sensor> sensors = sensorRepository.findByMachineId(machineId);

        List<PredictionFactor> factors = sensors.stream()
                .filter(s -> s.getLastReadingValue() != null && s.getMinRange() != null && s.getMaxRange() != null)
                .map(this::toFactor)
                .sorted(Comparator.comparingDouble(PredictionFactor::getDeviationPercent).reversed())
                .limit(5)
                .toList();

        String riskLevel = latest != null ? latest.getRiskLevel() : "UNKNOWN";
        String summary = buildSummary(riskLevel, factors);

        return PredictionExplanation.builder()
                .machineId(machineId)
                .riskLevel(riskLevel)
                .failureProbability(latest != null ? latest.getFailureProbability() : null)
                .confidenceScore(latest != null ? latest.getConfidenceScore() : null)
                .predictedAt(latest != null ? latest.getTimestamp() : null)
                .summary(summary)
                .topFactors(factors)
                .hasSensorData(!factors.isEmpty())
                .build();
    }

    private PredictionFactor toFactor(Sensor sensor) {
        double value = sensor.getLastReadingValue();
        double min = sensor.getMinRange();
        double max = sensor.getMaxRange();
        double rangeWidth = Math.max(max - min, 0.0001);

        double deviation = 0.0;
        if (value < min) {
            deviation = (min - value) / rangeWidth * 100.0;
        } else if (value > max) {
            deviation = (value - max) / rangeWidth * 100.0;
        }

        String contribution = deviation >= 25 ? "high" : deviation >= 5 ? "medium" : "low";

        return PredictionFactor.builder()
                .sensorName(sensor.getName())
                .sensorType(sensor.getType())
                .currentValue(value)
                .normalMin(min)
                .normalMax(max)
                .unit(sensor.getUnit())
                .deviationPercent(Math.round(deviation * 10.0) / 10.0)
                .contribution(contribution)
                .build();
    }

    private String buildSummary(String riskLevel, List<PredictionFactor> factors) {
        if (factors.isEmpty() || factors.get(0).getDeviationPercent() <= 0) {
            return "All monitored sensors are within their normal operating range. Risk level (" + riskLevel
                    + ") reflects the model's broader trend analysis, not a single out-of-range reading.";
        }
        PredictionFactor top = factors.get(0);
        return String.format(
                "%s sensor is the largest contributor — reading %.1f%s vs. a normal range of %.1f–%.1f%s (%.0f%% outside range).",
                top.getSensorName(), top.getCurrentValue(), safeUnit(top.getUnit()),
                top.getNormalMin(), top.getNormalMax(), safeUnit(top.getUnit()), top.getDeviationPercent());
    }

    private String safeUnit(String unit) {
        return unit == null ? "" : " " + unit;
    }
}
