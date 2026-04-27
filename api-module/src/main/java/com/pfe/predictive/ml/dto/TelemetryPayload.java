package com.pfe.predictive.ml.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO for ingesting real-time sensor telemetry data from industrial equipment.
 * This is the primary API contract for factories/sensors to send data to Spring Boot.
 * 
 * The payload structure aligns with C-MAPSS NASA turbofan dataset:
 * - 3 operational settings (T2, T24, T30)
 * - 21 sensor readings (continuous monitoring)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Sensor telemetry payload for ML prediction")
public class TelemetryPayload {

    @NotNull(message = "Timestamp is required")
    @Schema(
        description = "When this telemetry was recorded (ISO 8601 format)",
        example = "2026-04-20T14:30:00",
        required = true
    )
    private LocalDateTime timestamp;

    @NotNull(message = "Operational settings are required")
    @Schema(description = "3 operational settings from the machine", required = true)
    private OperationalSettings operationalSettings;

    @NotNull(message = "Sensor readings are required")
    @Schema(
        description = "21 sensor readings as key-value pairs. Keys must be 'sensor1' through 'sensor21'",
        example = "{\"sensor1\": 370.5, \"sensor2\": 45.2, ...}",
        required = true
    )
    private Map<String, Double> sensorReadings;

    /**
     * Operational settings for the machine.
     * These affect how the ML model interprets sensor data.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Operational settings")
    public static class OperationalSettings {
        
        @NotNull(message = "Setting1 is required")
        @Schema(description = "Operating condition 1 (T2)", example = "560.5", required = true)
        private Double setting1;
        
        @NotNull(message = "Setting2 is required")
        @Schema(description = "Operating condition 2 (T24)", example = "0.42", required = true)
        private Double setting2;
        
        @NotNull(message = "Setting3 is required")
        @Schema(description = "Operating condition 3 (T30)", example = "0.75", required = true)
        private Double setting3;
    }
}
