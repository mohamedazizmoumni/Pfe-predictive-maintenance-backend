package com.pfe.predictive.alert.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO used when escalating an alert to a manager.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EscalateAlertRequest {

    @Size(max = 1000, message = "Notes cannot exceed 1000 characters")
    private String escalationNotes;

    @Size(max = 100, message = "Assigned to username must be 100 characters or less")
    private String reassignTo;
}
