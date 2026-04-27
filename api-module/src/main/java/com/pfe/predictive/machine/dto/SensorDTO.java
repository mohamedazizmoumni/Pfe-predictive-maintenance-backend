package com.pfe.predictive.machine.dto;

import java.time.LocalDateTime;

public class SensorDTO {
    private Long id;
    private String code;
    private String name;
    private String description;
    private String type;
    private String unit;
    private Double minRange;
    private Double maxRange;
    private String status;
    private LocalDateTime lastReadingTime;
    private Double lastReadingValue;
    private Long machineId;

    public SensorDTO() {}

    public SensorDTO(Long id, String code, String name, String type, Long machineId) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.type = type;
        this.machineId = machineId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public Double getMinRange() {
        return minRange;
    }

    public void setMinRange(Double minRange) {
        this.minRange = minRange;
    }

    public Double getMaxRange() {
        return maxRange;
    }

    public void setMaxRange(Double maxRange) {
        this.maxRange = maxRange;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getLastReadingTime() {
        return lastReadingTime;
    }

    public void setLastReadingTime(LocalDateTime lastReadingTime) {
        this.lastReadingTime = lastReadingTime;
    }

    public Double getLastReadingValue() {
        return lastReadingValue;
    }

    public void setLastReadingValue(Double lastReadingValue) {
        this.lastReadingValue = lastReadingValue;
    }

    public Long getMachineId() {
        return machineId;
    }

    public void setMachineId(Long machineId) {
        this.machineId = machineId;
    }
}
