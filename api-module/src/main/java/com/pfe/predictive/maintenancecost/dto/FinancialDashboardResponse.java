package com.pfe.predictive.maintenancecost.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinancialDashboardResponse {

    private BigDecimal totalAllocated;
    private BigDecimal totalSpent;
    private BigDecimal totalRemaining;
    private Double overallUtilization;
    
    private List<TopCostMachine> topCostMachines;
    private List<BudgetAlert> alerts;
    private List<MonthlyCost> monthlyCosts;
    private List<BudgetResponse> budgetsByDepartment;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopCostMachine {
        private Long machineId;
        private String machineName;
        private BigDecimal totalCost;
        private BigDecimal maintenanceCost;
        private BigDecimal failureCost;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BudgetAlert {
        private String department;
        private String period;
        private String alertType;
        private String message;
        private BigDecimal amount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyCost {
        private String month;
        private BigDecimal maintenanceCost;
        private BigDecimal failureCost;
        private BigDecimal totalCost;
    }
}
