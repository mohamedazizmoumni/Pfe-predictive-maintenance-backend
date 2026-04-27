package com.pfe.predictive.ml.controller;

import com.pfe.predictive.core.entity.Machine;
import com.pfe.predictive.data.repository.MachineRepository;
import com.pfe.predictive.ml.dto.PredictionRecordDTO;
import com.pfe.predictive.ml.dto.TelemetryPayload;
import com.pfe.predictive.ml.mapper.PredictionRecordMapper;
import com.pfe.predictive.ml.service.MlPredictionService;
import com.pfe.predictive.ml.service.TelemetryNormalizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST API endpoints for sensor telemetry ingestion and prediction triggering.
 * 
 * This is the primary API integration point for:
 * - Industrial sensors and IoT devices
 * - Edge computing platforms
 * - Real-time monitoring systems
 * 
 * Workflow:
 * 1. POST /api/v1/machines/{id}/telemetry - receive sensor readings
 * 2. Normalize features to ML input format
 * 3. Call ML predictive service
 * 4. Persist prediction record with audit trail
 * 5. Return prediction result
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/machines/{machineId}/telemetry")
@Tag(name = "Telemetry Ingestion", description = "Sensor data ingestion and ML prediction triggering")
public class TelemetryIngestController {

    private final MachineRepository machineRepository;
    private final TelemetryNormalizationService normalizationService;
    private final MlPredictionService mlPredictionService;
    private final PredictionRecordMapper predictionMapper;

    public TelemetryIngestController(
            MachineRepository machineRepository,
            TelemetryNormalizationService normalizationService,
            MlPredictionService mlPredictionService,
            PredictionRecordMapper predictionMapper) {
        this.machineRepository = machineRepository;
        this.normalizationService = normalizationService;
        this.mlPredictionService = mlPredictionService;
        this.predictionMapper = predictionMapper;
    }

    /**
     * Ingest real-time sensor telemetry and trigger ML prediction.
     * 
     * This endpoint:
     * 1. Validates that machine exists
     * 2. Normalizes sensor readings to [0, 1] using training data statistics
     * 3. Calls FastAPI predictive maintenance model
     * 4. Persists prediction record for audit trail and historical analysis
     * 5. Returns prediction result with record ID
     * 
     * @param machineId ID of the machine sending telemetry
     * @param payload telemetry data (3 operational settings + 21 sensor readings)
     * @param authentication current user context
     * @return prediction record with RUL and risk level
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'MANAGER', 'DATA_SCIENTIST', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(
        summary = "Ingest sensor telemetry and predict RUL",
        description = "Normalizes sensor data, calls ML model, persists result with audit trail"
    )
    public ResponseEntity<?> ingestTelemetry(
            @PathVariable Long machineId,
            @Valid @RequestBody TelemetryPayload payload,
            Authentication authentication) {

        try {
            // Validate machine exists
            Machine machine = machineRepository.findById(machineId)
                    .orElseThrow(() -> new IllegalArgumentException("Machine not found: " + machineId));

            System.out.println("Received telemetry for machine " + machineId + " from user " + authentication.getName());

            // Normalize telemetry to ML features
            List<Double> normalizedFeatures = normalizationService.normalize(payload);
            List<List<Double>> featureBatch = List.of(normalizedFeatures);

            // Generate correlation ID and request ID for tracing
            String correlationId = UUID.randomUUID().toString();
            String requestId = UUID.randomUUID().toString();

            // Predict and persist result
            String inputSummary = generateFeatureSummary(normalizedFeatures);
            MlPredictionService.PredictionRecordWithResponse result = mlPredictionService.predictAndPersist(
                    machineId,
                    featureBatch,
                    correlationId,
                    requestId,
                    authentication.getName(),
                    inputSummary
            );

            PredictionRecordDTO response = predictionMapper.toDTO(result.getRecord());

            System.out.println("Successfully processed telemetry for machine " + machineId + " - recordId=" + result.getRecordId());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            System.out.println("Invalid telemetry request for machine " + machineId + ": " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            System.out.println("Error processing telemetry for machine " + machineId + ": " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Prediction service error: " + e.getMessage()));
        }
    }

    /**
     * Get the expected schema for telemetry payloads.
     * Useful for API consumers to understand the contract and validate their data.
     * 
     * @return schema documentation with field names and valid ranges
     */
    @GetMapping("/schema")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Get telemetry schema",
        description = "Returns expected sensor field names and valid ranges for integration"
    )
    public ResponseEntity<Map<String, Object>> getTelemetrySchema() {
        return ResponseEntity.ok(Map.of(
            "operationalSettings", Map.of(
                "setting1", Map.of("description", "Operating condition 1 (T2 temperature)", "min", 518.67, "max", 598.67),
                "setting2", Map.of("description", "Operating condition 2 (T24 pressure/speed)", "min", 0.0, "max", 0.84),
                "setting3", Map.of("description", "Operating condition 3 (T30 altitude)", "min", 0.0, "max", 1.0)
            ),
            "sensorReadings", Map.of(
                "count", 21,
                "description", "21 temperature and pressure sensors from turbofan engine",
                "rangeMin", 0.0,
                "rangeMax", 100.0
            )
        ));
    }

    /**
     * Generate a summary of key input features for the prediction record.
     * This is used for debugging and understanding which sensor values drove the prediction.
     */
    private String generateFeatureSummary(List<Double> normalizedFeatures) {
        if (normalizedFeatures == null || normalizedFeatures.isEmpty()) {
            return "{}";
        }
        // Take first 5 features (operational settings + first 2 sensors)
        return String.format(
            "{\"setting1\": %.4f, \"setting2\": %.4f, \"setting3\": %.4f, \"sensor1\": %.4f, \"sensor2\": %.4f}",
            normalizedFeatures.get(0),
            normalizedFeatures.get(1),
            normalizedFeatures.get(2),
            normalizedFeatures.size() > 3 ? normalizedFeatures.get(3) : 0.0,
            normalizedFeatures.size() > 4 ? normalizedFeatures.get(4) : 0.0
        );
    }

    /**
     * Schema documentation DTO.
     */
    @lombok.Data
    @lombok.Builder
    @Schema(description = "Telemetry API schema documentation")
    public static class TelemetrySchema {
        private OperationalSettingsSchema operationalSettings;
        private SensorSchema sensorReadings;
        private Map<String, Object> example;

        @lombok.Data
        @lombok.Builder
        public static class OperationalSettingsSchema {
            private FieldSchema setting1;
            private FieldSchema setting2;
            private FieldSchema setting3;
        }

        @lombok.Data
        @lombok.Builder
        public static class SensorSchema {
            private int count;
            private String keyFormat;
            private String description;
            private Double rangeMin;
            private Double rangeMax;
        }

        @lombok.Data
        @lombok.Builder
        public static class FieldSchema {
            private String description;
            private Double min;
            private Double max;
        }
    }
}
