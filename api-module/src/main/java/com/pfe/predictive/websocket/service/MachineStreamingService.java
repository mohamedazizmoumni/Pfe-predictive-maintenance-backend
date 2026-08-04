package com.pfe.predictive.websocket.service;

import com.pfe.predictive.alert.dto.CreateAlertRequest;
import com.pfe.predictive.alert.entity.Alert;
import com.pfe.predictive.alert.entity.AlertCategory;
import com.pfe.predictive.alert.entity.AlertSeverity;
import com.pfe.predictive.alert.service.AlertBroadcastContext;
import com.pfe.predictive.alert.service.AlertDecision;
import com.pfe.predictive.alert.service.AlertDecisionEngine;
import com.pfe.predictive.alert.service.AlertDecisionInput;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class MachineStreamingService {

    private static final String ISSUE_TYPE_SENSOR_ANOMALY = "SENSOR_ANOMALY";

    private final MachineRepository machineRepository;
    private final SensorTelemetryRepository sensorTelemetryRepository;
    private final PredictionHistoryRepository predictionHistoryRepository;
    private final DatasetReplayEngine datasetReplayEngine;
    private final TelemetryMapper telemetryMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final MLServiceClient mlServiceClient;
    private final AlertService alertService;
    private final AlertDecisionEngine alertDecisionEngine;

    public MachineStreamingService(
            MachineRepository machineRepository,
            SensorTelemetryRepository sensorTelemetryRepository,
            PredictionHistoryRepository predictionHistoryRepository,
            DatasetReplayEngine datasetReplayEngine,
            TelemetryMapper telemetryMapper,
            SimpMessagingTemplate messagingTemplate,
            MLServiceClient mlServiceClient,
            AlertService alertService,
            AlertDecisionEngine alertDecisionEngine) {
        this.machineRepository = machineRepository;
        this.sensorTelemetryRepository = sensorTelemetryRepository;
        this.predictionHistoryRepository = predictionHistoryRepository;
        this.datasetReplayEngine = datasetReplayEngine;
        this.telemetryMapper = telemetryMapper;
        this.messagingTemplate = messagingTemplate;
        this.mlServiceClient = mlServiceClient;
        this.alertService = alertService;
        this.alertDecisionEngine = alertDecisionEngine;
    }

    // Deliberately not @Transactional: processMachine() makes a blocking HTTP
    // call to the ML service per machine. Wrapping this loop in one transaction
    // held a DB connection open across N sequential ML calls against a 5-10
    // connection Hikari pool - a handful of machines was enough to exhaust it.
    // Each repository .save() below already commits its own short transaction.
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

    // Same reasoning as streamReplayTick(): don't hold a transaction across the
    // blocking ML call.
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
        MLPredictionResponse prediction = mlServiceClient.getPrediction(machine.getId(), mlRequest, status.health());

        telemetryMapper.applyStatusToMachine(machine, status);
        machineRepository.save(machine);

        EnrichedMachineTelemetryDto enrichedTelemetry = telemetryMapper.toEnrichedTelemetryDto(machine, row, status, prediction);
        storePredictionHistory(machine, row, status, prediction);
        checkAndTriggerAnomalyAlert(enrichedTelemetry, machine);

        return Optional.of(enrichedTelemetry);
    }

    private void checkAndTriggerAnomalyAlert(EnrichedMachineTelemetryDto telemetry, Machine machine) {
        try {
            double health = telemetry.getHealth() != null ? telemetry.getHealth() : 100.0;

            AlertDecisionInput input = new AlertDecisionInput(
                    machine.getName(),
                    health,
                    telemetry.getPredictedRUL(),
                    telemetry.getAnomalyProbability(),
                    telemetry.getRiskLevel(),
                    telemetry.getFailureProbability(),
                    telemetry.getAnomalyType(),
                    telemetry.getPredictedFailureType(),
                    Boolean.TRUE.equals(telemetry.getRequiresImmediateAction())
            );
            AlertDecision decision = alertDecisionEngine.decide(input);
            boolean hasIssue = decision.severity() != AlertSeverity.INFO;

            AlertBroadcastContext context = new AlertBroadcastContext(
                    machine.getName(), health, telemetry.getAnomalyProbability(), telemetry.getRiskLevel(),
                    telemetry.getPredictedRUL(), telemetry.getFailureProbability(), telemetry.getAnomalyType(),
                    telemetry.getPredictedFailureType());

            Optional<Alert> activeIncident = alertService.findActiveAlert(machine.getId(), ISSUE_TYPE_SENSOR_ANOMALY);

            if (!hasIssue) {
                // Health ticked back up. Do NOT auto-close the incident — the sensor
                // feed is a noisy simulated signal and will cross the threshold
                // repeatedly. Auto-closing here would let it reopen (and re-email)
                // on the next dip. The incident stays open until a manager formally
                // closes it via the alert workflow (acknowledge/assign -> resolved
                // maintenance -> close), which is the only thing that unlocks a new
                // incident + email for this machine/issue.
                return;
            }

            if (activeIncident.isEmpty()) {
                // New incident — create it, send the email once.
                CreateAlertRequest alertRequest = CreateAlertRequest.builder()
                        .machineId(machine.getId())
                        .title(decision.title())
                        .message(decision.message())
                        .severity(decision.severity())
                        .category(AlertCategory.SENSOR_ANOMALY)
                        .sourceReference("ML_PREDICTION_" + telemetry.getTimestamp())
                        .recommendations(decision.recommendedAction())
                        .build();

                alertService.openIncident(alertRequest, ISSUE_TYPE_SENSOR_ANOMALY, "SYSTEM_ML", context);
                log.warn("Incident opened for machine {} — severity={}, health={}%, riskLevel={}",
                        machine.getId(), decision.severity(), health, telemetry.getRiskLevel());
            } else if (activeIncident.get().getSeverity() != decision.severity()) {
                // Incident already open and still ongoing — update in place, no email.
                alertService.updateIncidentSeverity(
                        activeIncident.get(), decision.severity(), decision.title(), decision.message(),
                        decision.recommendedAction(), context);
            }
            // else: incident open, severity unchanged — do nothing (no email, no duplicate alert).

        } catch (org.springframework.dao.DataIntegrityViolationException dup) {
            // Two callers (the 2s scheduler and a manual /streaming/trigger) can both
            // pass the findActiveAlert() check for the same machine+issueType before
            // either commits. alerts_active_incident (V46) is a partial unique index
            // on (machine_id, issue_type) WHERE is_active = true, so only one INSERT
            // wins; the loser fails here — before it ever sends an email or broadcast.
            // This is the race resolving itself correctly, not a failure.
            log.info("Anomaly incident for machine {} already opened concurrently — skipping duplicate", machine.getId());
        } catch (Exception ex) {
            log.error("Failed to trigger anomaly alert for machine {}: {}", machine.getId(), ex.getMessage());
        }
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
}
