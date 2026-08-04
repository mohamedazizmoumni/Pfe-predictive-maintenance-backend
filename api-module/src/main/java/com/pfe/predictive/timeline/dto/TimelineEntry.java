package com.pfe.predictive.timeline.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimelineEntry {
    private LocalDateTime timestamp;
    /** MAINTENANCE_SCHEDULED, MAINTENANCE_COMPLETED, ALERT, AUDIT, COMMENT */
    private String category;
    private String title;
    private String description;
    private String actor;
}
