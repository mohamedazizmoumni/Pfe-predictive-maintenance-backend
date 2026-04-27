package com.pfe.predictive.maintenancecost.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@Table(name = "maintenance_budgets")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceBudget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String department;

    @Column(nullable = false, length = 30)
    private String period;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal allocatedAmount;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal spentAmount;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal remainingAmount;

    @PrePersist
    @PreUpdate
    public void computeRemainingAmount() {
        BigDecimal allocated = allocatedAmount != null ? allocatedAmount : BigDecimal.ZERO;
        BigDecimal spent = spentAmount != null ? spentAmount : BigDecimal.ZERO;
        this.remainingAmount = allocated.subtract(spent).setScale(4, RoundingMode.HALF_UP);
    }
}