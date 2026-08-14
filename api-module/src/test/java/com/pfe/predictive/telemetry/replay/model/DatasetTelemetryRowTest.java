package com.pfe.predictive.telemetry.replay.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the hand-written parts of this record: the compact constructor's
 * null/defensive-copy handling, sensor(int)'s bounds check, and the
 * content-based equals/hashCode/toString overrides (records default to
 * reference identity for array components, which is why these exist at
 * all - see the class-level comment).
 */
class DatasetTelemetryRowTest {

    private DatasetTelemetryRow newRow(double[] sensors) {
        return new DatasetTelemetryRow("traj-1", "FD001", 1, 5, 0.1, 0.2, 0.3, sensors);
    }

    @Test
    void nullSensorsBecomeAnEmptyArrayNotNull() {
        DatasetTelemetryRow row = newRow(null);

        assertEquals(0, row.sensors().length);
    }

    @Test
    void constructorDefensivelyCopiesTheSensorsArray() {
        double[] original = {10.0, 20.0, 30.0};
        DatasetTelemetryRow row = newRow(original);

        original[0] = 999.0;

        assertEquals(10.0, row.sensors()[0], 0.0001);
    }

    @Test
    void sensorReturnsTheOneIndexedReading() {
        DatasetTelemetryRow row = newRow(new double[]{10.0, 20.0, 30.0});

        assertEquals(10.0, row.sensor(1), 0.0001);
        assertEquals(30.0, row.sensor(3), 0.0001);
    }

    @Test
    void sensorReturnsZeroWhenIndexIsOutOfBounds() {
        DatasetTelemetryRow row = newRow(new double[]{10.0, 20.0});

        assertEquals(0.0, row.sensor(0), 0.0001);
        assertEquals(0.0, row.sensor(-1), 0.0001);
        assertEquals(0.0, row.sensor(3), 0.0001);
    }

    @Test
    void rowsWithEqualValuesButDistinctArrayInstancesAreEqual() {
        DatasetTelemetryRow first = newRow(new double[]{1.0, 2.0});
        DatasetTelemetryRow second = newRow(new double[]{1.0, 2.0});

        assertTrue(first.equals(second));
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    void rowsWithDifferentSensorValuesAreNotEqual() {
        DatasetTelemetryRow first = newRow(new double[]{1.0, 2.0});
        DatasetTelemetryRow second = newRow(new double[]{1.0, 999.0});

        assertNotEquals(first, second);
    }

    @Test
    void notEqualToAnUnrelatedType() {
        DatasetTelemetryRow row = newRow(new double[]{1.0});

        assertFalse(row.equals("not a row"));
    }

    @Test
    void toStringIncludesTrajectoryAndSensorReadings() {
        DatasetTelemetryRow row = newRow(new double[]{1.5});

        String text = row.toString();

        assertTrue(text.contains("traj-1"));
        assertTrue(text.contains("1.5"));
    }
}
