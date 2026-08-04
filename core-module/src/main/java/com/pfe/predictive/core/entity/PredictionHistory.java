package com.pfe.predictive.core.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Prediction History Entity
 * 
 * Stores historical ML predictions and telemetry snapshots for:
 * - Trend analysis
 * - Performance monitoring
 * - Model retraining
 * - Audit trail
 * - Analytics dashboards
 */
@Entity
@Table(name = "prediction_history", indexes = {
    @Index(name = "idx_prediction_machine_id", columnList = "machine_id"),
    @Index(name = "idx_prediction_timestamp", columnList = "timestamp"),
    @Index(name = "idx_prediction_risk_level", columnList = "risk_level")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PredictionHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // Machine Reference
    @Column(name = "machine_id", nullable = false)
    private Long machineId;
    
    @Column(nullable = false)
    private LocalDateTime timestamp;
    
    // ==================== TELEMETRY SNAPSHOT ====================
    
    // Health Metrics
    private Double health;
    private Double riskScore;
    private Double remainingUsefulLife;
    
    // Sensor Readings
    private Double temperature;
    private Double vibration;
    private Double powerConsumption;
    private Double pressure;
    private Double current;
    private Double voltage;
    
    // Degradation State
    private Double bearingWear;
    private Double thermalStress;
    private Double lubricationLevel;
    private Double fatigueIndex;
    private Double efficiencyScore;
    
    // Operational Settings
    private Double loadFactor;
    private Double operatingSpeed;
    private Double ambientTemperature;
    
    // ==================== ML PREDICTION RESULTS ====================
    
    @Column(name = "predicted_rul")
    private Double predictedRUL;
    
    @Column(name = "anomaly_probability")
    private Double anomalyProbability;
    
    @Column(name = "risk_level", length = 20)
    private String riskLevel;
    
    @Column(name = "failure_probability")
    private Double failureProbability;
    
    @Column(name = "predicted_failure_type", length = 50)
    private String predictedFailureType;
    
    @Column(name = "recommended_action", length = 500)
    private String recommendedAction;
    
    @Column(name = "confidence_score")
    private Double confidenceScore;
    
    @Column(name = "anomaly_type", length = 50)
    private String anomalyType;
    
    private Double severity;
    
    @Column(name = "requires_immediate_action")
    private Boolean requiresImmediateAction;
    
    // ==================== METADATA ====================
    
    @Column(name = "ml_prediction_available")
    private Boolean mlPredictionAvailable;

    @Column(name = "model_version", length = 50)
    private String modelVersion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}
