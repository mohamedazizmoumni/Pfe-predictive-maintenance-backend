package com.pfe.predictive.core.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Time-series sensor data readings
 */
@Entity
@Table(name = "sensor_data", indexes = {
    @Index(name = "idx_sensor_data_sensor_id", columnList = "sensor_id"),
    @Index(name = "idx_sensor_data_timestamp", columnList = "recorded_date DESC")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SensorData {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sensor_id", nullable = false)
    private Sensor sensor;

    @Column
    private Double value;

    @Column(length = 50)
    private String unit;

    @Column(name = "recorded_date")
    private LocalDateTime recordedDate;

    @Column
    private Boolean isAnomaly = false;

    @CreationTimestamp
    @Column(nullable = false, updatable = false, name = "created_date")
    private LocalDateTime createdDate;

    @Version
    private Long version;
}
