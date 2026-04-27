package com.pfe.predictive.maintenancecost.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Entity
@Table(name = "failure_events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FailureEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "machine_id", nullable = false)
    private Machine machine;

    @Column(nullable = false, length = 255)
    private String failureType;

    @Column(nullable = false)
    private Double actualDowntimeHours;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal totalCostIncurred;

    @Column(nullable = false)
    private LocalDateTime occurredAt;

    @PrePersist
    @PreUpdate
    public void computeTotalCostIncurred() {
        BigDecimal hourlyValue = machine != null && machine.getHourlyProductionValue() != null
                ? machine.getHourlyProductionValue()
                : BigDecimal.ZERO;

        BigDecimal downtime = actualDowntimeHours != null
                ? BigDecimal.valueOf(actualDowntimeHours)
                : BigDecimal.ZERO;

        this.totalCostIncurred = downtime
                .multiply(hourlyValue)
                .setScale(4, RoundingMode.HALF_UP);
    }
}
