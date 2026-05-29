package com.pfe.predictive.telemetry.replay.model;

import java.util.List;

public record DatasetTrajectory(
        String trajectoryId,
        String sourceName,
        int unitId,
        List<DatasetTelemetryRow> rows) {

    public DatasetTrajectory {
        rows = List.copyOf(rows);
    }

    public int totalCycles() {
        return rows.size();
    }

    public DatasetTelemetryRow rowAt(int index) {
        if (rows.isEmpty()) {
            throw new IllegalStateException("Trajectory has no replay rows: " + trajectoryId);
        }
        int safeIndex = Math.max(0, Math.min(index, rows.size() - 1));
        return rows.get(safeIndex);
    }

    public DatasetTelemetryRow firstRow() {
        return rowAt(0);
    }

    public DatasetTelemetryRow lastRow() {
        return rowAt(rows.size() - 1);
    }
}
