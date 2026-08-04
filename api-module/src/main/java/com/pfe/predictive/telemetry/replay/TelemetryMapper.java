package com.pfe.predictive.telemetry.replay;

import com.pfe.predictive.core.entity.Machine;
import com.pfe.predictive.core.entity.MachineStatus;
import com.pfe.predictive.core.entity.PredictionHistory;
import com.pfe.predictive.core.entity.SensorTelemetry;
import com.pfe.predictive.ml.dto.MlPredictionRequest;
import com.pfe.predictive.ml.dto.MlTelemetryRecord;
import com.pfe.predictive.ml.dto.MLPredictionResponse;
import com.pfe.predictive.telemetry.replay.model.DatasetTelemetryRow;
import com.pfe.predictive.telemetry.replay.model.DatasetTrajectory;
import com.pfe.predictive.telemetry.replay.model.MachineTrajectoryStatus;
import com.pfe.predictive.websocket.dto.EnrichedMachineTelemetryDto;
import com.pfe.predictive.websocket.dto.MachineTelemetryDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class TelemetryMapper {

    private static final DateTimeFormatter PYTHON_TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS");
    private final TelemetryReplayProperties properties;

    public TelemetryMapper(TelemetryReplayProperties properties) {
        this.properties = properties;
    }

    public SensorTelemetry toSensorTelemetry(DatasetTelemetryRow row, Machine machine, LocalDateTime timestamp) {
        return SensorTelemetry.builder()
                .machineId(machine.getId())
                .timestamp(timestamp)
                .setting1(row.setting1())
                .setting2(row.setting2())
                .setting3(row.setting3())
                .sensor1(row.sensor(1))
                .sensor2(row.sensor(2))
                .sensor3(row.sensor(3))
                .sensor4(row.sensor(4))
                .sensor5(row.sensor(5))
                .sensor6(row.sensor(6))
                .sensor7(row.sensor(7))
                .sensor8(row.sensor(8))
                .sensor9(row.sensor(9))
                .sensor10(row.sensor(10))
                .sensor11(row.sensor(11))
                .sensor12(row.sensor(12))
                .sensor13(row.sensor(13))
                .sensor14(row.sensor(14))
                .sensor15(row.sensor(15))
                .sensor16(row.sensor(16))
                .sensor17(row.sensor(17))
                .sensor18(row.sensor(18))
                .sensor19(row.sensor(19))
                .sensor20(row.sensor(20))
                .sensor21(row.sensor(21))
                .build();
    }

    public MachineTelemetryDto toMachineTelemetryDto(Machine machine, DatasetTelemetryRow row, MachineTrajectoryStatus status) {
        return MachineTelemetryDto.builder()
                .machineId(machine.getId())
                .serialNumber(machine.getSerialNumber())
                .name(machine.getName())
                .status(resolveStatus(status))
                .health(status.health())
                .remainingUsefulLife(status.remainingUsefulLife())
                .riskScore(status.riskScore())
                .bearingWear(deriveBearingWear(status))
                .thermalStress(deriveThermalStress(status))
                .lubricationLevel(deriveLubricationLevel(status))
                .fatigueIndex(deriveFatigueIndex(status))
                .efficiencyScore(deriveEfficiencyScore(status))
                .temperature(deriveTemperature(status))
                .vibration(row.sensor(2))
                .powerConsumption(row.sensor(3))
                .pressure(row.sensor(4))
                .acousticEmission(row.sensor(15))
                .current(row.sensor(17))
                .voltage(row.sensor(7))
                .rotationSpeed(row.sensor(9))
                .efficiency(row.sensor(12))
                .ambientTemperature(row.setting1())
                .loadFactor(row.setting2())
                .operatingSpeed(row.setting3())
                .operatingHours(status.currentCycle() * properties.getCycleDurationHours())
                .isCritical(status.failureDetected())
                .isDegrading(status.health() <= properties.getDegradingHealthThreshold())
                .timestamp(LocalDateTime.now())
                .build();
    }

    public EnrichedMachineTelemetryDto toEnrichedTelemetryDto(
            Machine machine,
            DatasetTelemetryRow row,
            MachineTrajectoryStatus status,
            MLPredictionResponse prediction) {

        return EnrichedMachineTelemetryDto.builder()
                .machineId(machine.getId())
                .serialNumber(machine.getSerialNumber())
                .name(machine.getName())
                .status(resolveStatus(status))
                .health(status.health())
                .remainingUsefulLife(status.remainingUsefulLife())
                .riskScore(status.riskScore())
                .bearingWear(deriveBearingWear(status))
                .thermalStress(deriveThermalStress(status))
                .lubricationLevel(deriveLubricationLevel(status))
                .fatigueIndex(deriveFatigueIndex(status))
                .efficiencyScore(deriveEfficiencyScore(status))
                .temperature(deriveTemperature(status))
                .vibration(row.sensor(2))
                .powerConsumption(row.sensor(3))
                .pressure(row.sensor(4))
                .acousticEmission(row.sensor(15))
                .current(row.sensor(17))
                .voltage(row.sensor(7))
                .rotationSpeed(row.sensor(9))
                .efficiency(row.sensor(12))
                .ambientTemperature(row.setting1())
                .loadFactor(row.setting2())
                .operatingSpeed(row.setting3())
                .operatingHours(status.currentCycle() * properties.getCycleDurationHours())
                .isCritical(status.failureDetected())
                .isDegrading(status.health() <= properties.getDegradingHealthThreshold())
                .predictedRUL(prediction.getPredictedRUL())
                .anomalyProbability(prediction.getAnomalyProbability())
                .riskLevel(prediction.getRiskLevel())
                .failureProbability(prediction.getFailureProbability())
                .predictedFailureType(prediction.getPredictedFailureType())
                .recommendedAction(prediction.getRecommendedAction())
                .confidenceScore(prediction.getConfidenceScore())
                .anomalyType(prediction.getAnomalyType())
                .severity(prediction.getSeverity())
                .requiresImmediateAction(prediction.getRequiresImmediateAction())
                .timestamp(LocalDateTime.now())
                .mlPredictionAvailable(isRealPrediction(prediction))
                .modelVersion(prediction.getModelVersion())
                .build();
    }

    /**
     * A prediction is "real" (trained-model output) unless MLServiceClient had to
     * substitute its rule-based fallback, which it always tags modelVersion="fallback-*".
     * Callers use this to disclose fallback state instead of presenting it as a live prediction.
     */
    private boolean isRealPrediction(MLPredictionResponse prediction) {
        String modelVersion = prediction.getModelVersion();
        return modelVersion != null && !modelVersion.startsWith("fallback");
    }

    public MlPredictionRequest toMlPredictionRequest(Machine machine, DatasetTelemetryRow row, MachineTrajectoryStatus status) {
        int cycle = (int) Math.max(0L, Math.round(status.currentCycle() * properties.getCycleDurationHours()));

        List<Double> sensors = new ArrayList<>(21);
        for (int sensorIndex = 1; sensorIndex <= 21; sensorIndex++) {
            sensors.add(row.sensor(sensorIndex));
        }

        MlTelemetryRecord telemetryRecord = MlTelemetryRecord.builder()
            .machineId(machine.getId())
            .timestamp(LocalDateTime.now().format(PYTHON_TIMESTAMP))
                .cycle(cycle)
                .sensors(sensors)
                .build();

        return MlPredictionRequest.builder()
                .telemetry(List.of(telemetryRecord))
                .build();
    }

    public MachineTrajectoryStatus buildStatusSnapshot(Long machineId, DatasetTrajectory trajectory, DatasetTelemetryRow row, int currentIndex) {
        int totalCycles = trajectory.totalCycles();
        double progress = totalCycles <= 1 ? 1.0d : (double) currentIndex / (double) (totalCycles - 1);
        double health = clamp(100.0d - (progress * 90.0d));
        double riskScore = clamp(100.0d - health);
        double remainingUsefulLife = Math.max(0.0d, (totalCycles - currentIndex - 1L) * properties.getCycleDurationHours());
        boolean failureDetected = currentIndex >= totalCycles - 1 || health <= properties.getCriticalHealthThreshold();
        boolean paused = failureDetected && properties.isPauseAtEndOfTrajectory();
        boolean completed = failureDetected;

        return new MachineTrajectoryStatus(
                machineId,
                trajectory.trajectoryId(),
                trajectory.sourceName(),
                trajectory.unitId(),
                currentIndex,
                row.cycle(),
                totalCycles,
                progress,
                1.0d,
                paused,
                completed,
                failureDetected,
                health,
                riskScore,
                remainingUsefulLife,
                LocalDateTime.now());
    }

    public void applyStatusToMachine(Machine machine, MachineTrajectoryStatus status) {
        machine.setOperatingHours(status.currentCycle() * properties.getCycleDurationHours());
        machine.setRiskScore(status.riskScore());

        if (status.failureDetected()) {
            machine.setStatus(MachineStatus.FAULTY.name());
        } else if (status.health() <= properties.getCriticalHealthThreshold()) {
            machine.setStatus(MachineStatus.MAINTENANCE.name());
        } else if (status.health() <= properties.getDegradingHealthThreshold()) {
            machine.setStatus(MachineStatus.MAINTENANCE.name());
        } else {
            machine.setStatus(MachineStatus.OPERATIONAL.name());
        }
    }

    public PredictionHistory toPredictionHistory(Machine machine, DatasetTelemetryRow row, MachineTrajectoryStatus status, MLPredictionResponse prediction) {
        return PredictionHistory.builder()
                .machineId(machine.getId())
                .timestamp(LocalDateTime.now())
                .health(status.health())
                .riskScore(status.riskScore())
                .remainingUsefulLife(status.remainingUsefulLife())
                .temperature(deriveTemperature(status))
                .vibration(row.sensor(2))
                .powerConsumption(row.sensor(3))
                .pressure(row.sensor(4))
                .current(row.sensor(17))
                .voltage(row.sensor(7))
                .bearingWear(deriveBearingWear(status))
                .thermalStress(deriveThermalStress(status))
                .lubricationLevel(deriveLubricationLevel(status))
                .fatigueIndex(deriveFatigueIndex(status))
                .efficiencyScore(deriveEfficiencyScore(status))
                .loadFactor(row.setting2())
                .operatingSpeed(row.setting3())
                .ambientTemperature(row.setting1())
                .predictedRUL(prediction.getPredictedRUL())
                .anomalyProbability(prediction.getAnomalyProbability())
                .riskLevel(prediction.getRiskLevel())
                .failureProbability(prediction.getFailureProbability())
                .predictedFailureType(prediction.getPredictedFailureType())
                .recommendedAction(prediction.getRecommendedAction())
                .confidenceScore(prediction.getConfidenceScore())
                .anomalyType(prediction.getAnomalyType())
                .severity(prediction.getSeverity())
                .requiresImmediateAction(prediction.getRequiresImmediateAction())
                .mlPredictionAvailable(isRealPrediction(prediction))
                .modelVersion(prediction.getModelVersion())
                .build();
    }

    private String resolveStatus(MachineTrajectoryStatus status) {
        if (status.failureDetected()) {
            return "FAILED";
        }
        if (status.health() <= properties.getCriticalHealthThreshold()) {
            return "CRITICAL";
        }
        if (status.health() <= properties.getDegradingHealthThreshold()) {
            return "DEGRADED";
        }
        return "ACTIVE";
    }

    private double deriveBearingWear(MachineTrajectoryStatus status) {
        return clamp(status.progress() * 100.0d);
    }

    private double deriveThermalStress(MachineTrajectoryStatus status) {
        return clamp(status.progress() * 85.0d);
    }

    private double deriveLubricationLevel(MachineTrajectoryStatus status) {
        return clamp(100.0d - (status.progress() * 80.0d));
    }

    private double deriveFatigueIndex(MachineTrajectoryStatus status) {
        return clamp(status.progress() * 95.0d);
    }

    private double deriveEfficiencyScore(MachineTrajectoryStatus status) {
        return clamp(100.0d - (status.progress() * 75.0d));
    }

    /**
     * Synthetic operating temperature in °C, driven by degradation progress.
     * NASA C-MAPSS's raw sensor columns are turbofan gas-path measurements
     * (Rankine-scale core/turbine temps, RPM, psia) with no realistic
     * mapping onto a factory machine's temperature gauge — sensor(8) in
     * particular is physical fan speed (~2388-2390), not a temperature at
     * all, which is why it was rendering as an absurd "2388°C". This
     * produces a believable 32-90°C range instead, consistent with the
     * other derive*() synthetic metrics below.
     */
    private double deriveTemperature(MachineTrajectoryStatus status) {
        return clamp(32.0d + status.progress() * 58.0d);
    }

    private double clamp(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 0.0d;
        }
        return Math.max(0.0d, Math.min(100.0d, value));
    }
}
