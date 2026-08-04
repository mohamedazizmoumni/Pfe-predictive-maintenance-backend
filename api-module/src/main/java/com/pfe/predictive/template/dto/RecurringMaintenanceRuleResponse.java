package com.pfe.predictive.template.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecurringMaintenanceRuleResponse {
    private Long id;
    private Long machineId;
    private Long workOrderTemplateId;
    private String workOrderTemplateName;
    private Integer intervalDays;
    private Long assignedTechnicianId;
    private LocalDateTime nextRunDate;
    private Long lastGeneratedMaintenanceId;
    private boolean active;
}
