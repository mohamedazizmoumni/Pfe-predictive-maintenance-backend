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
public class RejectExpenseRequest {

    @NotBlank(message = "Rejection reason is required")
    private String rejectionReason;
}
