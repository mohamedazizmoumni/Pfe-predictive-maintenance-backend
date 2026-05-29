package com.pfe.predictive.maintenancecost.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Budget Request DTO
 * Used for creating and updating budgets
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetRequest {

    @NotBlank(message = "Department is required")
    private String department;

    @NotBlank(message = "Period is required")
    @Pattern(regexp = "\\d{4}-Q[1-4]|\\d{4}-\\d{2}", 
             message = "Period must be in format YYYY-QX or YYYY-MM (e.g., 2026-Q2 or 2026-05)")
    private String period;

    @NotNull(message = "Allocated amount is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Allocated amount must be positive")
    private BigDecimal allocatedAmount;
}
