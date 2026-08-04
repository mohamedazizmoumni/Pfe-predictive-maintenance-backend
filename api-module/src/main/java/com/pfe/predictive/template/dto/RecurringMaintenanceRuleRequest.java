package com.pfe.predictive.template.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RecurringMaintenanceRuleRequest {
    @NotNull
    private Long machineId;
    @NotNull
    private Long workOrderTemplateId;
    @NotNull
    @Positive
    private Integer intervalDays;
    private Long assignedTechnicianId;
    private LocalDateTime firstRunDate;
    private Boolean active;
}
