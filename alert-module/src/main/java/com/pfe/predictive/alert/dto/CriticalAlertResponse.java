package com.pfe.predictive.alert.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Minimal payload for highlighting critical alerts.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CriticalAlertResponse {

    private Long id;
    private Long machineId;
    private String title;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdDate;

    private String assignedTo;
    private String recommendations;
    private int minutesSinceCreation;
}
