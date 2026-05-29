package com.pfe.predictive.core.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * LAYER 6: MACHINE EVENT
 * 
 * Stores detected events from real-time monitoring:
 * - Vibration spikes
 * - Temperature spikes
 * - Degradation acceleration
 * - Threshold crossings
 * - Anomaly detection
 */
@Entity
@Table(name = "machine_events", indexes = {
    @Index(name = "idx_machine_events_machine_time", columnList = "machine_id,event_timestamp DESC"),
    @Index(name = "idx_machine_events_type", columnList = "event_type"),
    @Index(name = "idx_machine_events_severity", columnList = "severity")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MachineEvent {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "machine_id", nullable = false)
    private Long machineId;
    
    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;  // VIBRATION_SPIKE, TEMPERATURE_SPIKE, DEGRADATION_ACCELERATION, etc.
    
    @Column(name = "event_timestamp", nullable = false)
    private LocalDateTime eventTimestamp;
    
    @Column(nullable = false, length = 20)
    private String severity;  // LOW, MEDIUM, HIGH, CRITICAL
    
    @Column(name = "sensor_name", length = 50)
    private String sensorName;  // e.g., "sensor2" (vibration)
    
    @Column(name = "sensor_value")
    private Double sensorValue;  // Actual sensor value at event time
    
    @Column(name = "threshold_value")
    private Double thresholdValue;  // Threshold that was crossed
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "action_taken", length = 255)
    private String actionTaken;  // e.g., "ML_REEVALUATION_TRIGGERED", "ALERT_SENT"
    
    @Column(nullable = false)
    private boolean acknowledged = false;
    
    @Column(name = "acknowledged_by", length = 255)
    private String acknowledgedBy;
    
    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    // ==================== EVENT TYPES ====================
    
    public static final String VIBRATION_SPIKE = "VIBRATION_SPIKE";
    public static final String TEMPERATURE_SPIKE = "TEMPERATURE_SPIKE";
    public static final String PRESSURE_DROP = "PRESSURE_DROP";
    public static final String DEGRADATION_ACCELERATION = "DEGRADATION_ACCELERATION";
    public static final String THRESHOLD_CROSSING = "THRESHOLD_CROSSING";
    public static final String ANOMALY_DETECTED = "ANOMALY_DETECTED";
    public static final String HEALTH_CRITICAL = "HEALTH_CRITICAL";
    public static final String BEARING_WEAR_HIGH = "BEARING_WEAR_HIGH";
    public static final String LUBRICATION_LOW = "LUBRICATION_LOW";
    
    // ==================== SEVERITY LEVELS ====================
    
    public static final String SEVERITY_LOW = "LOW";
    public static final String SEVERITY_MEDIUM = "MEDIUM";
    public static final String SEVERITY_HIGH = "HIGH";
    public static final String SEVERITY_CRITICAL = "CRITICAL";
    
    // ==================== HELPER METHODS ====================
    
    public void acknowledge(String username) {
        this.acknowledged = true;
        this.acknowledgedBy = username;
        this.acknowledgedAt = LocalDateTime.now();
    }
    
    public boolean isCritical() {
        return SEVERITY_CRITICAL.equals(severity);
    }
    
    public boolean isHigh() {
        return SEVERITY_HIGH.equals(severity);
    }
}
