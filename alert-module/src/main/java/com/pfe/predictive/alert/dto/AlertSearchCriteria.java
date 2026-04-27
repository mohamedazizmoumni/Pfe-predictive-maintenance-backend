package com.pfe.predictive.alert.dto;

import com.pfe.predictive.alert.entity.AlertSeverity;
import com.pfe.predictive.alert.entity.AlertStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Encapsulates optional filters accepted by the alert listing endpoint.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertSearchCriteria {

    private AlertStatus status;
    private AlertSeverity severity;
    private String assignedTo;
    private Boolean viewed;
    private String search;
}
