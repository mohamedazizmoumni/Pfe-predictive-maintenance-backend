package com.pfe.predictive.template.dto;

import com.pfe.predictive.core.entity.MaintenancePriority;
import com.pfe.predictive.core.entity.MaintenanceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class WorkOrderTemplateRequest {
    @NotBlank
    private String name;
    private String description;
    @NotNull
    private MaintenanceType type;
    @NotNull
    private MaintenancePriority priority;
    private Integer estimatedDuration;
    private String defaultNotes;
    private Boolean active;
}
