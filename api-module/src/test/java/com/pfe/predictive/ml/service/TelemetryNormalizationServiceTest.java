package com.pfe.predictive.ml.service;

import com.pfe.predictive.ml.dto.TelemetryPayload;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * KNOWN ISSUE, documented rather than silently masked: this service is
 * wired live into POST /api/v1/machines/{id}/telemetry (the documented
 * "primary API integration point for industrial sensors and IoT devices"),
 * but it does not actually read the payload — every call returns the same
 * 24 hardcoded 0.5 placeholders regardless of the real sensor values sent.
 * Real external telemetry therefore drives predictions off constant fake
 * input on this ingestion path. This test pins the current (broken)
 * contract so a future fix is a deliberate, visible change here rather
 * than a silent behavior shift.
 */
class TelemetryNormalizationServiceTest {

    private final TelemetryNormalizationService service = new TelemetryNormalizationService();

    @Test
    void rejectsNullPayload() {
        assertThrows(IllegalArgumentException.class, () -> service.normalize(null));
    }

    @Test
    void currentlyReturnsConstantPlaceholdersIgnoringActualSensorValues() {
        TelemetryPayload payload = new TelemetryPayload();

        List<Double> normalized = service.normalize(payload);

        assertEquals(24, normalized.size());
        assertEquals(java.util.Collections.nCopies(24, 0.5), normalized);
    }
}
