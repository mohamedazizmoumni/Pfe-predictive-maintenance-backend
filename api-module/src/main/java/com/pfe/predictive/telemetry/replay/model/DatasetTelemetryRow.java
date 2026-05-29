package com.pfe.predictive.telemetry.replay.model;

import java.util.Arrays;

public record DatasetTelemetryRow(
        String trajectoryId,
        String sourceName,
        int unitId,
        int cycle,
        double setting1,
        double setting2,
        double setting3,
        double[] sensors) {

    public DatasetTelemetryRow {
        sensors = sensors == null ? new double[0] : Arrays.copyOf(sensors, sensors.length);
    }

    public double sensor(int sensorNumber) {
        if (sensorNumber < 1 || sensorNumber > sensors.length) {
            return 0.0;
        }
        return sensors[sensorNumber - 1];
    }
}
