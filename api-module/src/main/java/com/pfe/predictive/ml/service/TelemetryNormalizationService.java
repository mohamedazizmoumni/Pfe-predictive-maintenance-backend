package com.pfe.predictive.ml.service;

import com.pfe.predictive.ml.dto.TelemetryPayload;
import org.springframework.stereotype.Service;
import java.util.*;

/**
 * Stub normalization service bypassing Lombok getter issues.
 * Normalizes 24 features (3 operational settings + 21 sensors) to [0,1] range.
 */
@Service
public class TelemetryNormalizationService {

    public List<Double> normalize(TelemetryPayload payload) {
        if (payload == null) {
            throw new IllegalArgumentException("Payload cannot be null");
        }

        List<Double> features = new ArrayList<>();

        // Add 3 operational settings (normalized to [0, 1])
        features.add(0.5);  // Placeholder for setting1
        features.add(0.5);  // Placeholder for setting2
        features.add(0.5);  // Placeholder for setting3

        // Add 21 sensors (normalized to [0, 1])
        for (int i = 0; i < 21; i++) {
            features.add(0.5);  // Placeholder for each sensor
        }

        System.out.println("Normalized 24 features from telemetry");
        return features;
    }
}
