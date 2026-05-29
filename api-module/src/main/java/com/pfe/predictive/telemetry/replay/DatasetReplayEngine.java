package com.pfe.predictive.telemetry.replay;

import com.pfe.predictive.core.entity.Machine;
import com.pfe.predictive.telemetry.replay.model.DatasetReplayFrame;
import com.pfe.predictive.telemetry.replay.model.DatasetTelemetryRow;
import com.pfe.predictive.telemetry.replay.model.DatasetTrajectory;
import com.pfe.predictive.telemetry.replay.model.MachineTrajectoryStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Slf4j
public class DatasetReplayEngine {

    private final MachineTrajectoryManager trajectoryManager;
    private final TelemetryMapper telemetryMapper;
    private final TelemetryReplayProperties replayProperties;
    private final ConcurrentMap<Long, Integer> fallbackCycleByMachine = new ConcurrentHashMap<>();
    private final AtomicBoolean fallbackNoticeLogged = new AtomicBoolean(false);

    public DatasetReplayEngine(
        MachineTrajectoryManager trajectoryManager,
        TelemetryMapper telemetryMapper,
        TelemetryReplayProperties replayProperties) {
        this.trajectoryManager = trajectoryManager;
        this.telemetryMapper = telemetryMapper;
    this.replayProperties = replayProperties;
    }

    public Optional<DatasetReplayFrame> advanceMachine(Machine machine) {
    try {
        Optional<DatasetTelemetryRow> replayRow = trajectoryManager.advance(machine.getId());
        if (replayRow.isEmpty()) {
        return Optional.empty();
        }

        DatasetTelemetryRow row = replayRow.get();
        DatasetTrajectory trajectory = trajectoryManager.trajectoryForMachine(machine.getId());
        MachineTrajectoryStatus status = telemetryMapper.buildStatusSnapshot(
            machine.getId(),
            trajectory,
            row,
            trajectoryManager.snapshot(machine.getId()).currentIndex());

        trajectoryManager.recordMetrics(
            machine.getId(),
            status.health(),
            status.riskScore(),
            status.remainingUsefulLife(),
            status.failureDetected());

        return Optional.of(new DatasetReplayFrame(
            machine,
            trajectory,
            row,
            status,
            telemetryMapper.toSensorTelemetry(row, machine, LocalDateTime.now()),
            telemetryMapper.toMachineTelemetryDto(machine, row, status)));
    } catch (IllegalStateException ex) {
        if (!isMissingDatasetException(ex)) {
        throw ex;
        }

            if (fallbackNoticeLogged.compareAndSet(false, true)) {
                log.warn("Dataset replay fallback activated because no NASA C-MAPSS trajectories were loaded. Streaming synthetic telemetry until dataset resources become available.");
            }
        return Optional.of(buildSyntheticFrame(machine));
        }
    }

    private boolean isMissingDatasetException(IllegalStateException ex) {
    String message = ex.getMessage();
    return message != null && message.contains("No NASA C-MAPSS trajectories loaded");
    }

    private DatasetReplayFrame buildSyntheticFrame(Machine machine) {
    int cycle = fallbackCycleByMachine.merge(machine.getId(), 1, Integer::sum);
    double phase = (cycle + Math.floorMod(machine.getId().intValue(), 97)) / 15.0d;
    double progress = (Math.sin(phase) + 1.0d) / 2.0d;
    double health = clamp(100.0d - progress * 85.0d);
    double riskScore = clamp(100.0d - health);
    double remainingUsefulLife = Math.max(0.0d, (100.0d - riskScore) * replayProperties.getCycleDurationHours());
    boolean failureDetected = health <= replayProperties.getCriticalHealthThreshold();

    double[] sensors = new double[] {
        45.0d + progress * 35.0d,
        2.0d + progress * 6.0d,
        35.0d + progress * 45.0d,
        70.0d + progress * 35.0d,
        20.0d + progress * 20.0d,
        7.0d + progress * 2.5d,
        215.0d + progress * 15.0d,
        0.0d,
        0.0d,
        0.0d,
        0.0d,
        0.0d,
        0.0d,
        0.0d,
        0.0d,
        0.0d,
        0.0d,
        1100.0d + progress * 400.0d,
        90.0d - progress * 25.0d,
        0.0d,
        0.0d
    };

    DatasetTelemetryRow row = new DatasetTelemetryRow(
        "fallback-machine-" + machine.getId(),
        "synthetic-fallback",
        Math.toIntExact(machine.getId()),
        cycle,
        25.0d + progress * 15.0d,
        0.5d + progress * 0.5d,
        0.7d + progress * 0.6d,
        sensors);

    DatasetTrajectory trajectory = new DatasetTrajectory(
        row.trajectoryId(),
        row.sourceName(),
        row.unitId(),
        List.of(row));

    MachineTrajectoryStatus status = new MachineTrajectoryStatus(
        machine.getId(),
        row.trajectoryId(),
        row.sourceName(),
        row.unitId(),
        cycle,
        cycle,
        Integer.MAX_VALUE,
        progress,
        replayProperties.getDefaultSpeed(),
        false,
        false,
        failureDetected,
        health,
        riskScore,
        remainingUsefulLife,
        LocalDateTime.now());

    return new DatasetReplayFrame(
        machine,
        trajectory,
        row,
        status,
        telemetryMapper.toSensorTelemetry(row, machine, LocalDateTime.now()),
        telemetryMapper.toMachineTelemetryDto(machine, row, status));
    }

    private double clamp(double value) {
    if (Double.isNaN(value) || Double.isInfinite(value)) {
        return 0.0d;
    }
    return Math.max(0.0d, Math.min(100.0d, value));
    }
}
