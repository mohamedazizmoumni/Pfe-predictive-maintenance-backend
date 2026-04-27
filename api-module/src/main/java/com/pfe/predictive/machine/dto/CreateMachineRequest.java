package com.pfe.predictive.machine.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateMachineRequest {
    @NotBlank(message = "Serial number is required")
    @JsonAlias({"machineSerial", "serial_number"})
    private String serialNumber;

    @NotBlank(message = "Machine name is required")
    @JsonAlias({"machineName", "machine_name"})
    private String name;

    @JsonAlias({"machineDescription", "description_text"})
    private String description;

    @NotBlank(message = "Model is required")
    @JsonAlias({"machineModel", "model_name"})
    private String model;

    @JsonAlias({"machineManufacturer", "manufacturer_name"})
    private String manufacturer;

    @NotBlank(message = "Location is required")
    @JsonAlias({"machineLocation", "location_name"})
    private String location;

    @JsonAlias({"installationYear", "install_year"})
    private Integer installationYear;

    @JsonAlias({"machineStatus", "status_value"})
    private String status;

    public CreateMachineRequest() {}

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
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

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Integer getInstallationYear() {
        return installationYear;
    }

    public void setInstallationYear(Integer installationYear) {
        this.installationYear = installationYear;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
