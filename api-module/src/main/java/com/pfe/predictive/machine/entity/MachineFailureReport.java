package com.pfe.predictive.machine.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "machine_failure_reports")
public class MachineFailureReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long machineId;

    @Column(nullable = false, length = 255)
    private String machineName;

    @Column(nullable = false, length = 4000)
    private String currentSensorState;

    @Column(nullable = false)
    private Integer predictedFailureDays;

    @Column(nullable = false)
    private Double risk;

    @Column(nullable = false, length = 1000)
    private String recommendedAction;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal estimatedCost;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<MachineFailureReportPart> requiredParts = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getMachineId() {
        return machineId;
    }

    public void setMachineId(Long machineId) {
        this.machineId = machineId;
    }

    public String getMachineName() {
        return machineName;
    }

    public void setMachineName(String machineName) {
        this.machineName = machineName;
    }

    public String getCurrentSensorState() {
        return currentSensorState;
    }

    public void setCurrentSensorState(String currentSensorState) {
        this.currentSensorState = currentSensorState;
    }

    public Integer getPredictedFailureDays() {
        return predictedFailureDays;
    }

    public void setPredictedFailureDays(Integer predictedFailureDays) {
        this.predictedFailureDays = predictedFailureDays;
    }

    public Double getRisk() {
        return risk;
    }

    public void setRisk(Double risk) {
        this.risk = risk;
    }

    public String getRecommendedAction() {
        return recommendedAction;
    }

    public void setRecommendedAction(String recommendedAction) {
        this.recommendedAction = recommendedAction;
    }

    public BigDecimal getEstimatedCost() {
        return estimatedCost;
    }

    public void setEstimatedCost(BigDecimal estimatedCost) {
        this.estimatedCost = estimatedCost;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<MachineFailureReportPart> getRequiredParts() {
        return requiredParts;
    }

    public void setRequiredParts(List<MachineFailureReportPart> requiredParts) {
        this.requiredParts = requiredParts;
    }
}
