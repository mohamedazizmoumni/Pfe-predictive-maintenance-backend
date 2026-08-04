package com.pfe.predictive.reliability.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FailureRecordDto {
    private Long maintenanceId;
    private Long machineId;
    private String machineName;
    private String type;
    private String description;
    private String rootCause;
    /** Hours between start and completion; null if either is missing. */
    private Double repairHours;
    private LocalDateTime completedDate;
}
