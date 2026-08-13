package com.pfe.predictive.executive.dto;

import com.pfe.predictive.reliability.dto.MachineReliabilityDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Cross-module rollup for the executive KPI widget on Super Admin/Admin
 * dashboards — "Plant Director" responsibilities expressed as a dashboard
 * section rather than a separate role (2026-08-01 scope reorientation).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutiveSummaryDto {
    private int machineCount;
    private Double fleetAverageHealth;
    private long openWorkOrders;
    private long overdueWorkOrders;
    private long pendingRapportApprovals;
    private long pendingExpenseApprovals;
    private long pendingReorderApprovals;
    private long openSupportTickets;
    /** Null if no budget exists for the current year. */
    private BigDecimal budgetUtilizationPercentage;
    /** Worst 3 machines by MTBF — see ReliabilityService. */
    private List<MachineReliabilityDto> topReliabilityRisks;
    /** Maintenance completed in the last 30 days, by type — see ExecutiveSummaryService. */
    private long preventiveCompletedLast30Days;
    private long correctiveCompletedLast30Days;
}
