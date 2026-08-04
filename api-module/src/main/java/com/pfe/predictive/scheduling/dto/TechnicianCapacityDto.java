package com.pfe.predictive.scheduling.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TechnicianCapacityDto {
    private Long technicianId;
    private String technicianName;
    private String username;
    /** Count of maintenance records assigned to this technician with status SCHEDULED or IN_PROGRESS. */
    private long openJobCount;
    /** Sum of estimatedDuration (hours) across those open jobs. */
    private Integer totalEstimatedHours;
}
