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
public class ExpenseSummaryResponse {

    private int year;
    private int month;
    private String monthName;
    private BigDecimal totalApprovedAmount;
    private BigDecimal totalPendingAmount;
    private BigDecimal totalRejectedAmount;
    private long approvedCount;
    private long pendingCount;
    private long rejectedCount;
    private long totalExpenseCount;
    private Map<String, BigDecimal> amountByCategory;
}
