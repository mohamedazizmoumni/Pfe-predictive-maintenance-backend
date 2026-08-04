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
public class MachineReliabilityDto {
    private Long machineId;
    private String machineName;
    /** Mean Time Between Failures, in hours. Null if fewer than 2 corrective/emergency repairs recorded. */
    private Double mtbfHours;
    /** Mean Time To Repair, in hours. Null if no repair has both a start and completed date. */
    private Double mttrHours;
    private Integer failureCount;
    private LocalDateTime lastFailureAt;
}
