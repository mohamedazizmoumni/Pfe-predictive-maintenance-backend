package com.pfe.predictive.alert.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.pfe.predictive.alert.entity.AlertSeverity;
import com.pfe.predictive.alert.entity.AlertStatus;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Lightweight projection used in alert list/grid views.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertDto {

    private Long id;
    private Long machineId;
    private String title;
    private AlertSeverity severity;
    private AlertStatus status;
    private String assignedTo;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdDate;

    private Boolean viewed;
}
