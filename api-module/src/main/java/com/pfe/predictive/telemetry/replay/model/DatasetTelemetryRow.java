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

    // Records auto-generate equals/hashCode/toString using reference identity
    // for array components, not content — overridden here so two rows with
    // the same sensor values (but distinct array instances) compare equal
    // and print their actual readings instead of an array memory reference.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DatasetTelemetryRow other)) return false;
        return unitId == other.unitId
                && cycle == other.cycle
                && Double.compare(setting1, other.setting1) == 0
                && Double.compare(setting2, other.setting2) == 0
                && Double.compare(setting3, other.setting3) == 0
                && java.util.Objects.equals(trajectoryId, other.trajectoryId)
                && java.util.Objects.equals(sourceName, other.sourceName)
                && Arrays.equals(sensors, other.sensors);
    }

    @Override
    public int hashCode() {
        int result = java.util.Objects.hash(trajectoryId, sourceName, unitId, cycle, setting1, setting2, setting3);
        return 31 * result + Arrays.hashCode(sensors);
    }

    @Override
    public String toString() {
        return "DatasetTelemetryRow[" +
                "trajectoryId=" + trajectoryId +
                ", sourceName=" + sourceName +
                ", unitId=" + unitId +
                ", cycle=" + cycle +
                ", setting1=" + setting1 +
                ", setting2=" + setting2 +
                ", setting3=" + setting3 +
                ", sensors=" + Arrays.toString(sensors) +
                ']';
    }
}
