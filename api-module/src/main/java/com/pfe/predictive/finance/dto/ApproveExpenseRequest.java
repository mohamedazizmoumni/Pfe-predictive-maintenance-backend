package com.pfe.predictive.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApproveExpenseRequest {

    /** Optional note from the finance manager when approving. */
    private String reviewNote;
}
