package com.pfe.predictive.telemetry.replay.model;

import com.pfe.predictive.core.entity.Machine;
import com.pfe.predictive.core.entity.SensorTelemetry;
import com.pfe.predictive.websocket.dto.MachineTelemetryDto;

public record DatasetReplayFrame(
        Machine machine,
        DatasetTrajectory trajectory,
        DatasetTelemetryRow row,
        MachineTrajectoryStatus status,
        SensorTelemetry sensorTelemetry,
        MachineTelemetryDto machineTelemetryDto) {
}
