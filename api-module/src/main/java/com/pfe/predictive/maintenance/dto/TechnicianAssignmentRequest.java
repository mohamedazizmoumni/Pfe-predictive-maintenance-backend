package com.pfe.predictive.maintenance.dto;

import jakarta.validation.constraints.NotNull;

public class TechnicianAssignmentRequest {

    @NotNull(message = "Technician identifier is required")
    private Long technicianId;

    public Long getTechnicianId() {
        return technicianId;
    }

    public void setTechnicianId(Long technicianId) {
        this.technicianId = technicianId;
    }
}
