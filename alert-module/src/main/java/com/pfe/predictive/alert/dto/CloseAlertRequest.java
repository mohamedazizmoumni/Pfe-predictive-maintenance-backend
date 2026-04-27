package com.pfe.predictive.alert.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO used to close an alert with resolution notes.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CloseAlertRequest {

    @NotBlank(message = "Resolution notes are required to close an alert")
    @Size(min = 4, max = 1000, message = "Resolution notes must be between 4 and 1000 characters")
    private String resolutionNotes;
}
