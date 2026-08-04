package com.pfe.predictive.websocket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Enriched Machine Telemetry DTO
 * 
 * Combines real-time telemetry with ML predictions and anomaly detection.
 * This is what gets broadcast to the frontend via WebSocket.
 * 
 * Contains:
 * - Live sensor data
 * - Degradation state
 * - ML predictions
 * - Anomaly detection results
 * - Risk assessment
 * - Recommended actions
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrichedMachineTelemetryDto {
    
    // ==================== MACHINE IDENTITY ====================
    
    private Long machineId;
    private String serialNumber;
    private String name;
    private String status;
    
    // ==================== HEALTH METRICS ====================
    
    private Double health;
    private Double remainingUsefulLife;
    private Double riskScore;
    
    // ==================== DEGRADATION STATE ====================
    
    private Double bearingWear;
    private Double thermalStress;
    private Double lubricationLevel;
    private Double fatigueIndex;
    private Double efficiencyScore;
    
    // ==================== SENSOR READINGS ====================
    
    private Double temperature;
    private Double vibration;
    private Double powerConsumption;
    private Double pressure;
    private Double acousticEmission;
    private Double current;
    private Double voltage;
    private Double rotationSpeed;
    private Double efficiency;
    
    // ==================== OPERATIONAL SETTINGS ====================
    
    private Double ambientTemperature;
    private Double loadFactor;
    private Double operatingSpeed;
    private Double operatingHours;
    
    // ==================== STATUS FLAGS ====================
    
    private Boolean isCritical;
    private Boolean isDegrading;
    
    // ==================== ML PREDICTIONS ====================
    
    private Double predictedRUL;              // ML-predicted Remaining Useful Life
    private Double anomalyProbability;        // ML anomaly detection score
    private String riskLevel;                 // LOW, MEDIUM, HIGH, CRITICAL
    private Double failureProbability;        // ML failure probability
    private String predictedFailureType;      // Predicted failure mode
    private String recommendedAction;         // ML-recommended action
    private Double confidenceScore;           // ML model confidence
    private String anomalyType;               // Type of detected anomaly
    private Double severity;                  // Anomaly severity
    private Boolean requiresImmediateAction;  // Urgent action flag
    
    // ==================== METADATA ====================
    
    private LocalDateTime timestamp;
    private Boolean mlPredictionAvailable;    // Was the trained model reachable, or is this a rule-based fallback?
    private String modelVersion;              // e.g. "v2.1" for a real model, "fallback-1.0" when the ML service was unreachable
}
