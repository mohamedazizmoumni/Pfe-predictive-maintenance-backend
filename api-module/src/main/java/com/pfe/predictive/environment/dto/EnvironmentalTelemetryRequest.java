package com.pfe.predictive.environment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Real environmental sensor reading (e.g. ESP32 + DHT11) for an existing machine.
 * Deliberately separate from TelemetryPayload - this never feeds the RUL model.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Real temperature/humidity reading from IoT hardware attached to a machine")
public class EnvironmentalTelemetryRequest {

    @NotNull(message = "Temperature is required")
    @Schema(description = "Temperature in Celsius", example = "28.5", required = true)
    private Double temperature;

    @NotNull(message = "Humidity is required")
    @Schema(description = "Relative humidity percentage", example = "62.0", required = true)
    private Double humidity;
}
