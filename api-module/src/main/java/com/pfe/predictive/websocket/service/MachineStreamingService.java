package com.pfe.predictive.websocket.service;

import com.pfe.predictive.alert.dto.CreateAlertRequest;
import com.pfe.predictive.alert.entity.AlertCategory;
import com.pfe.predictive.alert.entity.AlertSeverity;
import com.pfe.predictive.alert.service.AlertService;
import com.pfe.predictive.core.entity.Machine;
import com.pfe.predictive.core.entity.PredictionHistory;
import com.pfe.predictive.data.repository.MachineRepository;
import com.pfe.predictive.data.repository.PredictionHistoryRepository;
import com.pfe.predictive.data.repository.SensorTelemetryRepository;
import com.pfe.predictive.ml.dto.MlPredictionRequest;
import com.pfe.predictive.ml.dto.MLPredictionResponse;
import com.pfe.predictive.ml.service.MLServiceClient;
import com.pfe.predictive.telemetry.replay.DatasetReplayEngine;
import com.pfe.predictive.telemetry.replay.TelemetryMapper;
import com.pfe.predictive.telemetry.replay.model.DatasetReplayFrame;
import com.pfe.predictive.telemetry.replay.model.DatasetTelemetryRow;
import com.pfe.predictive.telemetry.replay.model.MachineTrajectoryStatus;
import com.pfe.predictive.websocket.dto.EnrichedMachineTelemetryDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class MachineStreamingService {

    private final MachineRepository machineRepository;
    private final SensorTelemetryRepository sensorTelemetryRepository;
    private final PredictionHistoryRepository predictionHistoryRepository;
    private final DatasetReplayEngine datasetReplayEngine;
    private final TelemetryMapper telemetryMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final MLServiceClient mlServiceClient;
    private final AlertService alertService;

    private final Map<Long, LocalDateTime> lastAlertTime = new ConcurrentHashMap<>();
    private static final long ALERT_COOLDOWN_MINUTES = 15;

    public MachineStreamingService(
            MachineRepository machineRepository,
            SensorTelemetryRepository sensorTelemetryRepository,
            PredictionHistoryRepository predictionHistoryRepository,
            DatasetReplayEngine datasetReplayEngine,
            TelemetryMapper telemetryMapper,
            SimpMessagingTemplate messagingTemplate,
            MLServiceClient mlServiceClient,
            AlertService alertService) {
        this.machineRepository = machineRepository;
        this.sensorTelemetryRepository = sensorTelemetryRepository;
        this.predictionHistoryRepository = predictionHistoryRepository;
        this.datasetReplayEngine = datasetReplayEngine;
        this.telemetryMapper = telemetryMapper;
        this.messagingTemplate = messagingTemplate;
        this.mlServiceClient = mlServiceClient;
        this.alertService = alertService;
    }

    @Transactional
    public void streamReplayTick() {
        try {
            List<Machine> machines = machineRepository.findAll();
            if (machines.isEmpty()) {
                log.debug("No machines to stream");
                return;
            }

            List<EnrichedMachineTelemetryDto> payload = new ArrayList<>();
            for (Machine machine : machines) {
                processMachine(machine).ifPresent(payload::add);
            }

            if (!payload.isEmpty()) {
                messagingTemplate.convertAndSend("/topic/machines", payload);
                log.debug("Streamed dataset-driven telemetry for {} machines", payload.size());
            }
        } catch (Exception ex) {
            log.error("Streaming tick failed: {}", ex.getMessage(), ex);
        }
    }

    public void triggerStreaming() {
        log.info("Manual replay trigger");
        streamReplayTick();
    }

    @Transactional
    public EnrichedMachineTelemetryDto streamSpecificMachine(Long machineId) {
        Machine machine = machineRepository.findById(machineId)
                .orElseThrow(() -> new IllegalArgumentException("Machine not found: " + machineId));

        return processMachine(machine)
                .map(telemetry -> {
                    messagingTemplate.convertAndSend("/topic/machines", List.of(telemetry));
                    return telemetry;
                })
                .orElseThrow(() -> new IllegalStateException("No dataset telemetry available for machine " + machineId));
    }

    private Optional<EnrichedMachineTelemetryDto> processMachine(Machine machine) {
        Optional<DatasetReplayFrame> frameOptional = datasetReplayEngine.advanceMachine(machine);
        if (frameOptional.isEmpty()) {
            return Optional.empty();
        }

        DatasetReplayFrame frame = frameOptional.get();
        DatasetTelemetryRow row = frame.row();
        MachineTrajectoryStatus status = frame.status();

        sensorTelemetryRepository.save(frame.sensorTelemetry());

        MlPredictionRequest mlRequest = telemetryMapper.toMlPredictionRequest(machine, row, status);
        MLPredictionResponse prediction = mlServiceClient.getPrediction(machine.getId(), mlRequest);

        telemetryMapper.applyStatusToMachine(machine, status);
        machineRepository.save(machine);

        EnrichedMachineTelemetryDto enrichedTelemetry = telemetryMapper.toEnrichedTelemetryDto(machine, row, status, prediction);
        storePredictionHistory(machine, row, status, prediction);
        checkAndTriggerAnomalyAlert(enrichedTelemetry, machine);

        return Optional.of(enrichedTelemetry);
    }

    private void checkAndTriggerAnomalyAlert(EnrichedMachineTelemetryDto telemetry, Machine machine) {
        try {
            boolean isCriticalAnomaly =
                    Boolean.TRUE.equals(telemetry.getRequiresImmediateAction()) ||
                    "CRITICAL".equals(telemetry.getRiskLevel()) ||
                    (telemetry.getAnomalyProbability() != null && telemetry.getAnomalyProbability() > 0.8);

            if (!isCriticalAnomaly) {
                return;
            }

            LocalDateTime lastAlert = lastAlertTime.get(machine.getId());
            if (lastAlert != null && lastAlert.plusMinutes(ALERT_COOLDOWN_MINUTES).isAfter(LocalDateTime.now())) {
                log.debug("Alert cooldown active for machine {} (last alert: {})", machine.getId(), lastAlert);
                return;
            }

            AlertSeverity severity = determineAlertSeverity(telemetry);
            CreateAlertRequest alertRequest = CreateAlertRequest.builder()
                    .machineId(machine.getId())
                    .title("CRITICAL ANOMALY DETECTED - " + machine.getName())
                    .message(buildAlertMessage(telemetry))
                    .severity(severity)
                    .category(AlertCategory.SENSOR_ANOMALY)
                    .sourceReference("ML_PREDICTION_" + telemetry.getTimestamp())
                    .build();

            alertService.createAlert(alertRequest, "SYSTEM_ML");
            lastAlertTime.put(machine.getId(), LocalDateTime.now());

            messagingTemplate.convertAndSend("/topic/alerts", Map.of(
                    "machineId", machine.getId(),
                    "machineName", machine.getName(),
                    "severity", severity.toString(),
                    "message", "Critical anomaly detected - immediate attention required",
                    "timestamp", LocalDateTime.now(),
                    "anomalyProbability", telemetry.getAnomalyProbability(),
                    "riskLevel", telemetry.getRiskLevel()
            ));

            log.warn("CRITICAL ANOMALY ALERT triggered for machine {}: anomalyProb={}, riskLevel={}, requiresAction={}",
                    machine.getId(),
                    telemetry.getAnomalyProbability(),
                    telemetry.getRiskLevel(),
                    telemetry.getRequiresImmediateAction());

        } catch (Exception ex) {
            log.error("Failed to trigger anomaly alert for machine {}: {}", machine.getId(), ex.getMessage());
        }
    }

    private AlertSeverity determineAlertSeverity(EnrichedMachineTelemetryDto telemetry) {
        if (Boolean.TRUE.equals(telemetry.getRequiresImmediateAction()) || "CRITICAL".equals(telemetry.getRiskLevel())) {
            return AlertSeverity.CRITICAL;
        }
        if ("HIGH".equals(telemetry.getRiskLevel()) || (telemetry.getAnomalyProbability() != null && telemetry.getAnomalyProbability() > 0.8)) {
            return AlertSeverity.WARNING;
        }
        return AlertSeverity.WARNING;
    }

    private String buildAlertMessage(EnrichedMachineTelemetryDto telemetry) {
        StringBuilder message = new StringBuilder();
        message.append("Machine requires immediate attention.\n\n");
        message.append("ML Analysis:\n");
        message.append(String.format("- Anomaly Probability: %.1f%%\n", safe(telemetry.getAnomalyProbability()) * 100));
        message.append(String.format("- Risk Level: %s\n", telemetry.getRiskLevel()));
        message.append(String.format("- Failure Probability: %.1f%%\n", safe(telemetry.getFailureProbability()) * 100));
        message.append(String.format("- Predicted Failure Type: %s\n", telemetry.getPredictedFailureType()));
        message.append(String.format("- Anomaly Type: %s\n", telemetry.getAnomalyType()));
        message.append("\nHealth Status:\n");
        message.append(String.format("- Current Health: %.1f%%\n", safe(telemetry.getHealth())));
        message.append(String.format("- Predicted RUL: %.0f cycles\n", safe(telemetry.getPredictedRUL())));
        message.append("\nRecommended Action:\n").append(telemetry.getRecommendedAction());
        return message.toString();
    }

    private void storePredictionHistory(Machine machine, DatasetTelemetryRow row, MachineTrajectoryStatus status, MLPredictionResponse prediction) {
        try {
            PredictionHistory history = telemetryMapper.toPredictionHistory(machine, row, status, prediction);
            predictionHistoryRepository.save(history);
            log.debug("Stored prediction history for machine {}", machine.getId());
        } catch (Exception ex) {
            log.error("Failed to store prediction history for machine {}: {}", machine.getId(), ex.getMessage());
        }
    }

    private double safe(Double value) {
        if (value == null || Double.isNaN(value) || Double.isInfinite(value)) {
            return 0.0d;
        }
        return value;
    }
}
