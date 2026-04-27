package com.pfe.predictive.prediction.entity;

import lombok.*;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "predictions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Prediction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "machine_id", nullable = false)
    private Long machineId;

    @Column(nullable = false, length = 100)
    private String modelName;

    @Column(nullable = false, length = 255)
    private String prediction; // e.g., "FAILURE_LIKELY", "NORMAL", "DEGRADING"

    @Column
    private Double confidence; // 0-100

    @Column
    private LocalDateTime predictedFailureDate;

    @Column(length = 1000)
    private String recommendation;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @Version
    private Long version;
}

@Entity
@Table(name = "ml_models")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MLModel {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String modelName;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ModelStatus status; // TRAINING, DEPLOYED, DEPRECATED

    @Column
    private Double accuracy;

    @Column
    private Double precision;

    @Column
    private Double recall;

    @Column
    private LocalDateTime lastTrainedDate;

    @Column
    private LocalDateTime deployedDate;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @Version
    private Long version;
}

public enum ModelStatus {
    TRAINING,
    DEPLOYED,
    DEPRECATED
}
