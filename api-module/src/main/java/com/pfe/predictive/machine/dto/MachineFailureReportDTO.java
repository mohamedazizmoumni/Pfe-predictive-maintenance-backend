package com.pfe.predictive.machine.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class MachineFailureReportDTO {
    private Long id;
    private Long machineId;
    private String machineName;
    private String currentSensorState;
    private Integer predictedFailureDays;
    private Double risk;
    private List<RequiredPartDTO> requiredParts;
    private String recommendedAction;
    private BigDecimal estimatedCost;
    private LocalDateTime createdAt;

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

    public List<RequiredPartDTO> getRequiredParts() {
        return requiredParts;
    }

    public void setRequiredParts(List<RequiredPartDTO> requiredParts) {
        this.requiredParts = requiredParts;
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
}
