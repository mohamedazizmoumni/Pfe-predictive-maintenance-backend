package com.pfe.predictive.template.dto;

import com.pfe.predictive.core.entity.MaintenancePriority;
import com.pfe.predictive.core.entity.MaintenanceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkOrderTemplateResponse {
    private Long id;
    private String name;
    private String description;
    private MaintenanceType type;
    private MaintenancePriority priority;
    private Integer estimatedDuration;
    private String defaultNotes;
    private boolean active;
    private LocalDateTime createdDate;
}
