package com.pfe.predictive.machine.dto;

import java.time.LocalDateTime;

public class MachineTechnicianDTO {
    private Long id;
    private Long machineId;
    private Long technicianId;
    private String technicianUsername;
    private String technicianDisplayName;
    private Long assignedById;
    private String assignedByUsername;
    private LocalDateTime assignedAt;

    public MachineTechnicianDTO() {}

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

    public Long getTechnicianId() {
        return technicianId;
    }

    public void setTechnicianId(Long technicianId) {
        this.technicianId = technicianId;
    }

    public String getTechnicianUsername() {
        return technicianUsername;
    }

    public void setTechnicianUsername(String technicianUsername) {
        this.technicianUsername = technicianUsername;
    }

    public String getTechnicianDisplayName() {
        return technicianDisplayName;
    }

    public void setTechnicianDisplayName(String technicianDisplayName) {
        this.technicianDisplayName = technicianDisplayName;
    }

    public Long getAssignedById() {
        return assignedById;
    }

    public void setAssignedById(Long assignedById) {
        this.assignedById = assignedById;
    }

    public String getAssignedByUsername() {
        return assignedByUsername;
    }

    public void setAssignedByUsername(String assignedByUsername) {
        this.assignedByUsername = assignedByUsername;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(LocalDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }
}
