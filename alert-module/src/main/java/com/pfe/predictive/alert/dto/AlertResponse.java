package com.pfe.predictive.alert.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.pfe.predictive.alert.entity.AlertCategory;
import com.pfe.predictive.alert.entity.AlertSeverity;
import com.pfe.predictive.alert.entity.AlertStatus;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Full alert payload returned to the frontend.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertResponse {

    private Long id;
    private Long machineId;
    private String title;
    private String message;
    private AlertSeverity severity;
    private AlertStatus status;
    private AlertCategory category;
    private String sourceReference;
    private Boolean viewed;
    private String assignedTo;
    private String assignedToDisplayName;
    private String createdBy;
    private String createdByDisplayName;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdDate;

    private String acknowledgedBy;
    private String acknowledgedByDisplayName;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime acknowledgedDate;

    private String escalatedBy;
    private String escalatedByDisplayName;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime escalatedDate;
    private String escalationNotes;

    private String closedBy;
    private String closedByDisplayName;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime closedDate;

    private String resolutionNotes;
    private String recommendations;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastModifiedDate;

    public String getStatusDisplay() {
        return switch (status) {
            case NEW -> "New (Not Acknowledged)";
            case ACKNOWLEDGED -> "Acknowledged";
            case ESCALATED -> "Escalated";
            case CLOSED -> "Closed";
        };
    }

    public String getSeverityColor() {
        return switch (severity) {
            case INFO -> "blue";
            case WARNING -> "yellow";
            case CRITICAL -> "red";
        };
    }
}
