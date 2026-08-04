package com.pfe.predictive.environment;

import com.pfe.predictive.environment.dto.EnvironmentalTelemetryDto;
import com.pfe.predictive.environment.dto.EnvironmentalTelemetryRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Ingestion endpoint for real environmental sensor data (e.g. ESP32 + DHT11)
 * attached to an existing machine, independent of the RUL/telemetry-replay pipeline.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/machines/{machineId}/environment")
@Tag(name = "Environmental Telemetry", description = "Real temperature/humidity ingestion and environment risk assessment")
public class EnvironmentalTelemetryController {

    private final EnvironmentalTelemetryService environmentalTelemetryService;

    public EnvironmentalTelemetryController(EnvironmentalTelemetryService environmentalTelemetryService) {
        this.environmentalTelemetryService = environmentalTelemetryService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'MANAGER', 'DATA_SCIENTIST', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(
        summary = "Ingest real environmental sensor reading",
        description = "Persists temperature/humidity, scores environment risk, and broadcasts over WebSocket"
    )
    public ResponseEntity<?> ingestEnvironment(
            @PathVariable Long machineId,
            @Valid @RequestBody EnvironmentalTelemetryRequest request) {
        try {
            EnvironmentalTelemetryDto dto = environmentalTelemetryService.ingest(machineId, request);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid environmental telemetry request for machine {}: {}", machineId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error processing environmental telemetry for machine {}: {}", machineId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Environment ingestion error: " + e.getMessage()));
        }
    }

    @GetMapping("/latest")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get latest environmental reading for a machine")
    public ResponseEntity<?> getLatest(@PathVariable Long machineId) {
        try {
            return ResponseEntity.ok(environmentalTelemetryService.getLatest(machineId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }
}
