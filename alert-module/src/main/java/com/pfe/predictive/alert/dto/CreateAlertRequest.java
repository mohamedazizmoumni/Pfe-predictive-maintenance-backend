package com.pfe.predictive.alert.dto;

import com.pfe.predictive.alert.entity.AlertCategory;
import com.pfe.predictive.alert.entity.AlertSeverity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload used to create a new alert entry from the dashboard/API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAlertRequest {

    @NotNull(message = "Machine ID is required")
    private Long machineId;

    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 255, message = "Title must be between 3 and 255 characters")
    private String title;

    @Size(max = 2000, message = "Message cannot exceed 2000 characters")
    private String message;

    @NotNull(message = "Severity is required")
    private AlertSeverity severity;

    private AlertCategory category;

    @Size(max = 100, message = "Source reference must be 100 characters or less")
    private String sourceReference;

    @Size(max = 100, message = "Assigned to username must be 100 characters or less")
    private String assignedTo;

    @Size(max = 1000, message = "Recommendations cannot exceed 1000 characters")
    private String recommendations;
}
