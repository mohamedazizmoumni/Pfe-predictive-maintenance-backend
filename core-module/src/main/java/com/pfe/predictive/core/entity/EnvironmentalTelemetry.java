package com.pfe.predictive.core.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Real environmental sensor readings (e.g. ESP32 + DHT11 temperature/humidity)
 * attached to an existing Machine as a second, independent telemetry stream.
 * Not consumed by the C-MAPSS-trained RUL model — see EnvironmentAnalysisClient.
 */
@Entity
@Table(name = "environmental_telemetry", indexes = {
    @Index(name = "idx_env_machine_timestamp", columnList = "machine_id,timestamp DESC")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnvironmentalTelemetry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "machine_id", nullable = false)
    private Machine machine;

    @Column(nullable = false)
    private Double temperature;

    @Column(nullable = false)
    private Double humidity;

    @Column(name = "risk_level", length = 20)
    private String riskLevel;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
