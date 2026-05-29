package com.pfe.predictive.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinanceDashboardResponse {

    private int currentYear;

    private BigDecimal totalBudget;
    private BigDecimal spentAmount;
    private BigDecimal remainingBudget;
    private BigDecimal utilizationPercentage;

    private long pendingCount;
    private long approvedCount;
    private long rejectedCount;

    private BigDecimal pendingAmount;
    private BigDecimal approvedAmount;

    /** Breakdown of approved expense amounts per category (all 5 categories always present). */
    private Map<String, BigDecimal> expensesByCategory;

    private long totalExpenseReports;

    /** False when no budget has been configured for the current year. */
    private boolean budgetExists;
}
