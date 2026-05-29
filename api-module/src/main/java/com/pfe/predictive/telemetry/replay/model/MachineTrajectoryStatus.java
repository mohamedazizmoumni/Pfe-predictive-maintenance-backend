package com.pfe.predictive.telemetry.replay.model;

import java.time.LocalDateTime;

public record MachineTrajectoryStatus(
        Long machineId,
        String trajectoryId,
        String sourceName,
        int unitId,
        int currentIndex,
        int currentCycle,
        int totalCycles,
        double progress,
        double speedMultiplier,
        boolean paused,
        boolean completed,
        boolean failureDetected,
        double health,
        double riskScore,
        double remainingUsefulLife,
        LocalDateTime lastUpdatedAt) {

    public double progressPercent() {
        return progress * 100.0;
    }

    public boolean isActive() {
        return !paused && !completed;
    }
}
