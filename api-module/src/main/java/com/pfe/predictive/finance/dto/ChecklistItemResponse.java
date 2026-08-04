package com.pfe.predictive.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChecklistItemResponse {

    /** Positional index within the rapport (checklist items have no independent identity). */
    private Long id;
    private String description;
    private boolean passed;
    private String notes;
}
