package com.pfe.predictive.telemetry.replay;

import com.pfe.predictive.telemetry.replay.model.DatasetTelemetryRow;
import com.pfe.predictive.telemetry.replay.model.DatasetTrajectory;
import com.pfe.predictive.telemetry.replay.model.MachineTrajectoryStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class MachineTrajectoryManager {

    private final DatasetLoaderService datasetLoaderService;
    private final TelemetryReplayProperties properties;
    private final Map<Long, MachineReplayState> machineStates = new ConcurrentHashMap<>();
    private final Map<String, Long> trajectoryAssignments = new ConcurrentHashMap<>();
    private final Map<String, DatasetTrajectory> trajectoryIndex = new LinkedHashMap<>();

    public MachineTrajectoryManager(DatasetLoaderService datasetLoaderService, TelemetryReplayProperties properties) {
        this.datasetLoaderService = datasetLoaderService;
        this.properties = properties;
    }

    @PostConstruct
    public void initialize() {
        loadTrajectories();
    }

    public synchronized void loadTrajectories() {
        trajectoryIndex.clear();
        trajectoryAssignments.clear();
        for (DatasetTrajectory trajectory : datasetLoaderService.loadTrajectories()) {
            trajectoryIndex.put(trajectory.trajectoryId(), trajectory);
        }
        log.info("Trajectory manager ready with {} dataset trajectories", trajectoryIndex.size());
    }

    public Optional<DatasetTelemetryRow> advance(Long machineId) {
        MachineReplayState state = machineStates.computeIfAbsent(machineId, this::assignNewState);
        DatasetTrajectory trajectory = trajectoryIndex.get(state.trajectoryId);
        if (trajectory == null || trajectory.totalCycles() == 0) {
            return Optional.empty();
        }

        synchronized (state) {
            if (state.paused) {
                state.lastUpdatedAt = LocalDateTime.now();
                return Optional.of(trajectory.rowAt(state.currentIndex));
            }

            if (state.completed) {
                if (properties.isAutoResetOnFailure()) {
                    resetInternal(state);
                } else {
                    state.lastUpdatedAt = LocalDateTime.now();
                    return Optional.of(trajectory.rowAt(state.currentIndex));
                }
            }

            state.stepAccumulator += Math.max(0.1d, state.speedMultiplier);
            int stepsToAdvance = (int) Math.floor(state.stepAccumulator);
            if (stepsToAdvance > 0) {
                state.currentIndex = Math.min(state.currentIndex + stepsToAdvance, trajectory.totalCycles() - 1);
                state.stepAccumulator -= stepsToAdvance;
            }

            if (state.currentIndex >= trajectory.totalCycles() - 1) {
                state.completed = true;
                state.failureDetected = true;
                if (properties.isPauseAtEndOfTrajectory()) {
                    state.paused = true;
                }
            }

            state.lastUpdatedAt = LocalDateTime.now();
            return Optional.of(trajectory.rowAt(state.currentIndex));
        }
    }

    public MachineTrajectoryStatus snapshot(Long machineId) {
        MachineReplayState state = machineStates.computeIfAbsent(machineId, this::assignNewState);
        DatasetTrajectory trajectory = trajectoryIndex.get(state.trajectoryId);
        if (trajectory == null || trajectory.totalCycles() == 0) {
            throw new IllegalStateException("No trajectory available for machine " + machineId);
        }

        synchronized (state) {
            DatasetTelemetryRow currentRow = trajectory.rowAt(state.currentIndex);
            int totalCycles = trajectory.totalCycles();
            double progress = totalCycles <= 1 ? 1.0d : (double) state.currentIndex / (double) (totalCycles - 1);

            return new MachineTrajectoryStatus(
                    machineId,
                    state.trajectoryId,
                    trajectory.sourceName(),
                    trajectory.unitId(),
                    state.currentIndex,
                    currentRow.cycle(),
                    totalCycles,
                    progress,
                    state.speedMultiplier,
                    state.paused,
                    state.completed,
                    state.failureDetected,
                    state.lastHealth,
                    state.lastRiskScore,
                    state.lastRemainingUsefulLife,
                    state.lastUpdatedAt);
        }
    }

    public List<MachineTrajectoryStatus> getAllStatuses() {
        if (machineStates.isEmpty()) {
            return List.of();
        }

        List<MachineTrajectoryStatus> statuses = new ArrayList<>();
        for (Long machineId : machineStates.keySet()) {
            try {
                statuses.add(snapshot(machineId));
            } catch (Exception ex) {
                log.debug("Unable to build trajectory snapshot for machine {}: {}", machineId, ex.getMessage());
            }
        }
        return Collections.unmodifiableList(statuses);
    }

    public void pause(Long machineId) {
        MachineReplayState state = machineStates.computeIfAbsent(machineId, this::assignNewState);
        synchronized (state) {
            state.paused = true;
            state.lastUpdatedAt = LocalDateTime.now();
        }
    }

    public void resume(Long machineId) {
        MachineReplayState state = machineStates.computeIfAbsent(machineId, this::assignNewState);
        synchronized (state) {
            state.paused = false;
            state.completed = false;
            state.failureDetected = false;
            state.lastUpdatedAt = LocalDateTime.now();
        }
    }

    public void setSpeed(Long machineId, double speedMultiplier) {
        MachineReplayState state = machineStates.computeIfAbsent(machineId, this::assignNewState);
        synchronized (state) {
            state.speedMultiplier = Math.max(0.1d, speedMultiplier);
            state.lastUpdatedAt = LocalDateTime.now();
        }
    }

    public void reset(Long machineId) {
        MachineReplayState state = machineStates.computeIfAbsent(machineId, this::assignNewState);
        synchronized (state) {
            resetInternal(state);
            state.paused = false;
            state.lastUpdatedAt = LocalDateTime.now();
        }
    }

    public DatasetTrajectory trajectoryForMachine(Long machineId) {
        MachineReplayState state = machineStates.computeIfAbsent(machineId, this::assignNewState);
        return trajectoryIndex.get(state.trajectoryId);
    }

    public void recordMetrics(Long machineId, double health, double riskScore, double remainingUsefulLife, boolean failureDetected) {
        MachineReplayState state = machineStates.computeIfAbsent(machineId, this::assignNewState);
        synchronized (state) {
            state.lastHealth = health;
            state.lastRiskScore = riskScore;
            state.lastRemainingUsefulLife = remainingUsefulLife;
            state.failureDetected = failureDetected;
            state.completed = failureDetected;
            state.lastUpdatedAt = LocalDateTime.now();
        }
    }

    private MachineReplayState assignNewState(Long machineId) {
        if (trajectoryIndex.isEmpty()) {
            throw new IllegalStateException("No NASA C-MAPSS trajectories loaded. Configure telemetry.replay.dataset-resources first.");
        }

        List<DatasetTrajectory> trajectories = new ArrayList<>(trajectoryIndex.values());
        DatasetTrajectory trajectory = trajectories.stream()
                .filter(candidate -> !trajectoryAssignments.containsKey(candidate.trajectoryId()))
                .findFirst()
                .orElseGet(() -> {
                    int selectedIndex = Math.floorMod(machineId.hashCode(), trajectories.size());
                    log.warn("All dataset trajectories are already assigned. Reusing trajectory '{}' for machine {}.",
                            trajectories.get(selectedIndex).trajectoryId(), machineId);
                    return trajectories.get(selectedIndex);
                });

        MachineReplayState state = new MachineReplayState();
        state.trajectoryId = trajectory.trajectoryId();
        state.speedMultiplier = properties.getDefaultSpeed();
        state.currentIndex = 0;
        state.lastUpdatedAt = LocalDateTime.now();
        trajectoryAssignments.put(trajectory.trajectoryId(), machineId);
        log.info("Assigned trajectory '{}' (unit {}) to machine {}", trajectory.trajectoryId(), trajectory.unitId(), machineId);
        return state;
    }

    private void resetInternal(MachineReplayState state) {
        state.currentIndex = 0;
        state.stepAccumulator = 0.0d;
        state.completed = false;
        state.failureDetected = false;
        state.lastHealth = 100.0d;
        state.lastRiskScore = 0.0d;
        state.lastRemainingUsefulLife = 0.0d;
    }

    private static class MachineReplayState {
        private String trajectoryId;
        private int currentIndex;
        private double stepAccumulator;
        private double speedMultiplier;
        private boolean paused;
        private boolean completed;
        private boolean failureDetected;
        private double lastHealth = 100.0d;
        private double lastRiskScore = 0.0d;
        private double lastRemainingUsefulLife = 0.0d;
        private LocalDateTime lastUpdatedAt;
    }
}
