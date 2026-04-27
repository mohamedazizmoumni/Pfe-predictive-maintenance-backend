package com.pfe.predictive.machine.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateSensorRequest {
    @NotBlank(message = "Sensor code is required")
    @JsonAlias({"sensorCode", "code_value"})
    private String code;

    @NotBlank(message = "Sensor name is required")
    @JsonAlias({"sensorName", "name_value"})
    private String name;

    @JsonAlias({"sensorDescription"})
    private String description;

    @NotBlank(message = "Sensor type is required")
    @JsonAlias({"sensorType"})
    private String type;

    private String unit;

    @JsonAlias({"minThreshold", "minimumThreshold"})
    private Double minRange;

    @JsonAlias({"maxThreshold", "maximumThreshold"})
    private Double maxRange;

    public CreateSensorRequest() {}

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
}
