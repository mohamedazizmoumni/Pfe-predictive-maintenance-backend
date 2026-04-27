package com.pfe.predictive.machine.dto;

import java.time.LocalDateTime;
import java.util.Map;

public class SimulatedReadingDTO {
    private Long machineId;
    private String machineName;
    private LocalDateTime timestamp;
    private Double usageHours;
    private Integer anomalyCount;
    private Double risk;
    private Integer predictedFailureDays;
    private Map<String, Double> sensorValues;

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

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Double getUsageHours() {
        return usageHours;
    }

    public void setUsageHours(Double usageHours) {
        this.usageHours = usageHours;
    }

    public Integer getAnomalyCount() {
        return anomalyCount;
    }

    public void setAnomalyCount(Integer anomalyCount) {
        this.anomalyCount = anomalyCount;
    }

    public Double getRisk() {
        return risk;
    }

    public void setRisk(Double risk) {
        this.risk = risk;
    }

    public Integer getPredictedFailureDays() {
        return predictedFailureDays;
    }

    public void setPredictedFailureDays(Integer predictedFailureDays) {
        this.predictedFailureDays = predictedFailureDays;
    }

    public Map<String, Double> getSensorValues() {
        return sensorValues;
    }

    public void setSensorValues(Map<String, Double> sensorValues) {
        this.sensorValues = sensorValues;
    }
}
