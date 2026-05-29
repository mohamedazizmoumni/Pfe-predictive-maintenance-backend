package com.pfe.predictive.maintenancecost.service;

import com.pfe.predictive.maintenancecost.dto.BudgetResponse;
import com.pfe.predictive.maintenancecost.dto.FailureReportResponse;
import com.pfe.predictive.maintenancecost.dto.FinancialDashboardResponse;
import com.pfe.predictive.maintenancecost.dto.MaintenanceReportResponse;
import com.pfe.predictive.maintenancecost.entity.FailureEvent;
import com.pfe.predictive.maintenancecost.entity.Machine;
import com.pfe.predictive.maintenancecost.entity.MaintenanceAction;
import com.pfe.predictive.maintenancecost.entity.MaintenanceBudget;
import com.pfe.predictive.maintenancecost.enums.MaintenanceActionStatus;
import com.pfe.predictive.maintenancecost.repository.FailureEventRepository;
import com.pfe.predictive.maintenancecost.repository.MachineRepository;
import com.pfe.predictive.maintenancecost.repository.MaintenanceActionRepository;
import com.pfe.predictive.maintenancecost.repository.MaintenanceBudgetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Financial Dashboard Service
 * Provides comprehensive financial analytics and reporting
 * 
 * @author Finance Module
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FinancialDashboardService {

    private final MaintenanceBudgetRepository budgetRepository;
    private final MachineRepository machineRepository;
    private final MaintenanceActionRepository actionRepository;
    private final FailureEventRepository failureRepository;
    private final MaintenanceCostService costService;

    /**
     * Get comprehensive financial dashboard
     * @return Dashboard data
     */
    public FinancialDashboardResponse getDashboard() {
        log.info("📊 Generating financial dashboard");

        List<MaintenanceBudget> allBudgets = budgetRepository.findAll();

        BigDecimal totalAllocated = allBudgets.stream()
            .map(MaintenanceBudget::getAllocatedAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalSpent = allBudgets.stream()
            .map(MaintenanceBudget::getSpentAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalRemaining = allBudgets.stream()
            .map(MaintenanceBudget::getRemainingAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        double overallUtilization = totalAllocated.compareTo(BigDecimal.ZERO) > 0
            ? totalSpent.divide(totalAllocated, 4, RoundingMode.HALF_UP)
                       .multiply(BigDecimal.valueOf(100))
                       .doubleValue()
            : 0.0;

        return FinancialDashboardResponse.builder()
            .totalAllocated(totalAllocated)
            .totalSpent(totalSpent)
            .totalRemaining(totalRemaining)
            .overallUtilization(overallUtilization)
            .topCostMachines(getTopCostMachines(10))
            .alerts(getBudgetAlerts())
            .monthlyCosts(getMonthlyCosts())
            .budgetsByDepartment(allBudgets.stream()
                .map(this::toBudgetResponse)
                .collect(Collectors.toList()))
            .build();
    }

    /**
     * Get top cost machines
     * @param limit Number of machines to return
     * @return List of top cost machines
     */
    public List<FinancialDashboardResponse.TopCostMachine> getTopCostMachines(int limit) {
        List<Machine> allMachines = machineRepository.findAll();

        return allMachines.stream()
            .map(machine -> {
                BigDecimal maintenanceCost = costService.getTotalMaintenanceCosts(machine.getId());
                BigDecimal failureCost = costService.getTotalFailureCosts(machine.getId());
                BigDecimal totalCost = maintenanceCost.add(failureCost);

                return FinancialDashboardResponse.TopCostMachine.builder()
                    .machineId(machine.getId())
                    .machineName(machine.getName())
                    .totalCost(totalCost)
                    .maintenanceCost(maintenanceCost)
                    .failureCost(failureCost)
                    .build();
            })
            .sorted((a, b) -> b.getTotalCost().compareTo(a.getTotalCost()))
            .limit(limit)
            .collect(Collectors.toList());
    }

    /**
     * Get budget alerts
     * @return List of alerts
     */
    public List<FinancialDashboardResponse.BudgetAlert> getBudgetAlerts() {
        List<MaintenanceBudget> allBudgets = budgetRepository.findAll();
        List<FinancialDashboardResponse.BudgetAlert> alerts = new ArrayList<>();

        for (MaintenanceBudget budget : allBudgets) {
            // Over budget alert
            if (budget.getRemainingAmount().compareTo(BigDecimal.ZERO) < 0) {
                alerts.add(FinancialDashboardResponse.BudgetAlert.builder()
                    .department(budget.getDepartment())
                    .period(budget.getPeriod())
                    .alertType("OVER_BUDGET")
                    .message("Budget exceeded by " + budget.getRemainingAmount().abs())
                    .amount(budget.getRemainingAmount().abs())
                    .build());
            }
            // Near limit alert (>90% utilization)
            else {
                double utilization = budget.getSpentAmount()
                    .divide(budget.getAllocatedAmount(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue();

                if (utilization > 90.0) {
                    alerts.add(FinancialDashboardResponse.BudgetAlert.builder()
                        .department(budget.getDepartment())
                        .period(budget.getPeriod())
                        .alertType("NEAR_LIMIT")
                        .message(String.format("Budget %.1f%% utilized", utilization))
                        .amount(budget.getRemainingAmount())
                        .build());
                }
            }
        }

        return alerts;
    }

    /**
     * Get monthly costs (last 12 months)
     * @return List of monthly costs
     */
    public List<FinancialDashboardResponse.MonthlyCost> getMonthlyCosts() {
        // Simplified implementation - returns empty list
        // In production, you would query by date ranges
        return new ArrayList<>();
    }

    /**
     * Get failure report
     * @return Failure report
     */
    public FailureReportResponse getFailureReport() {
        log.info("📋 Generating failure report");

        List<FailureEvent> allFailures = failureRepository.findAll();

        BigDecimal totalCost = allFailures.stream()
            .map(FailureEvent::getTotalCostIncurred)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        double totalDowntime = allFailures.stream()
            .mapToDouble(FailureEvent::getActualDowntimeHours)
            .sum();

        List<FailureReportResponse.FailureDetail> details = allFailures.stream()
            .map(failure -> FailureReportResponse.FailureDetail.builder()
                .id(failure.getId())
                .machineId(failure.getMachine().getId())
                .machineName(failure.getMachine().getName())
                .failureType(failure.getFailureType())
                .downtimeHours(failure.getActualDowntimeHours())
                .cost(failure.getTotalCostIncurred())
                .occurredAt(failure.getOccurredAt())
                .build())
            .sorted((a, b) -> b.getOccurredAt().compareTo(a.getOccurredAt()))
            .collect(Collectors.toList());

        return FailureReportResponse.builder()
            .totalFailures((long) allFailures.size())
            .totalCost(totalCost)
            .totalDowntimeHours(totalDowntime)
            .failures(details)
            .build();
    }

    /**
     * Get failure summary
     * @return Failure summary with statistics
     */
    public Map<String, Object> getFailureSummary() {
        log.info("📊 Generating failure summary");

        List<FailureEvent> allFailures = failureRepository.findAll();

        BigDecimal totalCost = allFailures.stream()
            .map(FailureEvent::getTotalCostIncurred)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        double totalDowntime = allFailures.stream()
            .mapToDouble(FailureEvent::getActualDowntimeHours)
            .sum();

        BigDecimal averageCost = allFailures.isEmpty() 
            ? BigDecimal.ZERO 
            : totalCost.divide(BigDecimal.valueOf(allFailures.size()), 4, RoundingMode.HALF_UP);

        // Group by failure type
        Map<String, Long> failuresByType = allFailures.stream()
            .collect(Collectors.groupingBy(FailureEvent::getFailureType, Collectors.counting()));

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalFailures", allFailures.size());
        summary.put("totalCost", totalCost);
        summary.put("averageCostPerFailure", averageCost);
        summary.put("totalDowntimeHours", totalDowntime);
        summary.put("failuresByType", failuresByType);
        summary.put("generatedAt", new Date());

        return summary;
    }

    /**
     * Get maintenance report
     * @return Maintenance report
     */
    public MaintenanceReportResponse getMaintenanceReport() {
        log.info("📋 Generating maintenance report");

        List<MaintenanceAction> allActions = actionRepository.findAll();

        List<MaintenanceAction> completedActions = allActions.stream()
            .filter(action -> action.getStatus() == MaintenanceActionStatus.COMPLETED)
            .collect(Collectors.toList());

        BigDecimal totalLaborCost = BigDecimal.ZERO;
        BigDecimal totalPartsCost = BigDecimal.ZERO;

        List<MaintenanceReportResponse.MaintenanceDetail> details = new ArrayList<>();

        for (MaintenanceAction action : completedActions) {
            BigDecimal laborCost = action.getLaborCostPerHour()
                .multiply(BigDecimal.valueOf(action.getEstimatedDurationHours()))
                .setScale(4, RoundingMode.HALF_UP);

            BigDecimal partsCost = action.getParts().stream()
                .map(part -> part.getUnitCost())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            totalLaborCost = totalLaborCost.add(laborCost);
            totalPartsCost = totalPartsCost.add(partsCost);

            details.add(MaintenanceReportResponse.MaintenanceDetail.builder()
                .id(action.getId())
                .machineId(action.getMachine().getId())
                .machineName(action.getMachine().getName())
                .type(action.getType().toString())
                .status(action.getStatus().toString())
                .durationHours(action.getEstimatedDurationHours())
                .laborCost(laborCost)
                .partsCost(partsCost)
                .totalCost(laborCost.add(partsCost))
                .scheduledDate(action.getScheduledDate())
                .build());
        }

        return MaintenanceReportResponse.builder()
            .totalActions((long) completedActions.size())
            .totalCost(totalLaborCost.add(totalPartsCost))
            .totalLaborCost(totalLaborCost)
            .totalPartsCost(totalPartsCost)
            .actions(details)
            .build();
    }

    // ==================== HELPER METHODS ====================

    private BudgetResponse toBudgetResponse(MaintenanceBudget budget) {
        double utilization = budget.getAllocatedAmount().compareTo(BigDecimal.ZERO) > 0
            ? budget.getSpentAmount()
                .divide(budget.getAllocatedAmount(), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue()
            : 0.0;

        return BudgetResponse.builder()
            .id(budget.getId())
            .department(budget.getDepartment())
            .period(budget.getPeriod())
            .allocatedAmount(budget.getAllocatedAmount())
            .spentAmount(budget.getSpentAmount())
            .remainingAmount(budget.getRemainingAmount())
            .utilizationPercentage(utilization)
            .isOverBudget(budget.getRemainingAmount().compareTo(BigDecimal.ZERO) < 0)
            .build();
    }
}
