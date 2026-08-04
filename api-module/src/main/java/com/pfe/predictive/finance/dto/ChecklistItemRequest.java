package com.pfe.predictive.finance.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChecklistItemRequest {

    @NotBlank(message = "Checklist item description is required")
    private String description;

    private boolean passed;

    private String notes;
}
