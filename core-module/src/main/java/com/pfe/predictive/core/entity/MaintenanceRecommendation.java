package com.pfe.predictive.core.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Persisted, actionable version of a generated MaintenanceRecommendationResponse
 * (which itself stays a pure in-memory DTO for the existing preview endpoint).
 * Saving one is what makes approve/reject/history possible.
 */
@Entity
@Table(name = "maintenance_recommendations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceRecommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long machineId;

    @Column(nullable = false, length = 20)
    private String urgencyLevel;

    @Column(nullable = false, length = 20)
    private String recommendedAction;

    @Column(length = 2000)
    private String justification;

    private BigDecimal estimatedCost;

    private BigDecimal estimatedSavings;

    private Double failureProbability;

    private Integer daysUntilFailure;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MaintenanceRecommendationStatus status;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime generatedAt;

    private String decidedBy;

    private LocalDateTime decidedAt;

    @Column(length = 1000)
    private String decisionNote;

    private Long resultingMaintenanceId;
}
