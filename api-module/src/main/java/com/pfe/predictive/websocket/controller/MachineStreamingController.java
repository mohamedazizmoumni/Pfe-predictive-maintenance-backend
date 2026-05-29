package com.pfe.predictive.websocket.controller;

import com.pfe.predictive.websocket.dto.EnrichedMachineTelemetryDto;
import com.pfe.predictive.websocket.service.MachineStreamingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Machine Streaming REST Controller
 * 
 * Provides REST endpoints for WebSocket streaming management and testing.
 * 
 * Endpoints:
 * - GET /api/v1/streaming/info: Get WebSocket connection info
 * - POST /api/v1/streaming/trigger: Manually trigger streaming cycle
 * - POST /api/v1/streaming/trigger/{machineId}: Stream specific machine
 * - GET /api/v1/streaming/health: Health check
 */
@RestController
@RequestMapping("/api/v1/streaming")
@Tag(name = "Machine Streaming", description = "Real-time machine telemetry streaming via WebSocket")
@Slf4j
public class MachineStreamingController {
    
    private final MachineStreamingService streamingService;
    
    public MachineStreamingController(MachineStreamingService streamingService) {
        this.streamingService = streamingService;
    }
    
    /**
     * Get WebSocket connection information.
     * 
     * Returns the WebSocket endpoint URL and subscription topic.
     */
    @GetMapping("/info")
    @Operation(summary = "Get WebSocket connection info")
    public ResponseEntity<Map<String, Object>> getConnectionInfo() {
        return ResponseEntity.ok(Map.of(
            "websocketEndpoint", "/ws-machine",
            "subscriptionTopic", "/topic/machines",
            "protocol", "STOMP over WebSocket",
            "fallback", "SockJS",
            "streamingInterval", "2 seconds",
            "sourceOfTruth", "NASA C-MAPSS dataset replay",
            "status", "active",
            "instructions", Map.of(
                "connect", "Connect to ws://localhost:8080/ws-machine",
                "subscribe", "Subscribe to /topic/machines",
                "receive", "Receive dataset-driven telemetry every 2 seconds"
            )
        ));
    }
    
    /**
     * Manually trigger streaming cycle.
     * 
     * Useful for testing - immediately broadcasts telemetry to all subscribers.
     */
    @PostMapping("/trigger")
    @Operation(summary = "Manually trigger streaming cycle")
    public ResponseEntity<Map<String, String>> triggerStreaming() {
        log.info("Manual streaming trigger requested");
        streamingService.triggerStreaming();
        
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", "Streaming cycle triggered",
            "topic", "/topic/machines"
        ));
    }
    
    /**
     * Stream telemetry for a specific machine.
     * 
     * Useful for testing - broadcasts enriched telemetry for a single machine.
     */
    @PostMapping("/trigger/{machineId}")
    @Operation(summary = "Stream specific machine telemetry with ML predictions")
    public ResponseEntity<EnrichedMachineTelemetryDto> streamSpecificMachine(@PathVariable Long machineId) {
        log.info("Streaming requested for machine: {}", machineId);
        EnrichedMachineTelemetryDto telemetry = streamingService.streamSpecificMachine(machineId);
        
        return ResponseEntity.ok(telemetry);
    }
    
    /**
     * Health check endpoint.
     */
    @GetMapping("/health")
    @Operation(summary = "Health check")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "healthy",
            "service", "Machine Streaming Service",
            "websocket", "active"
        ));
    }
}
