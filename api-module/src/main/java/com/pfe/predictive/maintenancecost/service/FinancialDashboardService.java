package com.pfe.predictive.maintenancecost.service;

import com.pfe.predictive.core.entity.Maintenance;
import com.pfe.predictive.core.entity.MaintenanceType;
import com.pfe.predictive.core.entity.finance.AnnualBudget;
import com.pfe.predictive.core.entity.finance.MaintenanceRapport;
import com.pfe.predictive.core.entity.finance.RapportStatus;
import com.pfe.predictive.data.repository.MaintenanceRepository;
import com.pfe.predictive.data.repository.finance.AnnualBudgetRepository;
import com.pfe.predictive.data.repository.finance.MaintenanceRapportRepository;
import com.pfe.predictive.maintenancecost.dto.BudgetResponse;
import com.pfe.predictive.maintenancecost.dto.FailureReportResponse;
import com.pfe.predictive.maintenancecost.dto.FinancialDashboardResponse;
import com.pfe.predictive.maintenancecost.dto.MaintenanceReportResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Financial Dashboard Service — powers the "Maintenance Cost Analytics" page.
 *
 * This used to read from a completely disconnected mock schema
 * (maintenancecost.entity.{Machine,MaintenanceAction,FailureEvent,MaintenanceBudget}),
 * seeded with 3 fake machines that didn't correspond to any real machine in
 * the app. It now reads from the real maintenance-work pipeline: an APPROVED
 * MaintenanceRapport is the actual record of completed, paid-for maintenance
 * work (a technician did the job, a manager and finance manager both signed
 * off on its cost) — that's the real analogue of a "completed maintenance
 * action" and is the only place real machine + real cost data actually meet.
 *
 * "Failures" are redefined as CORRECTIVE/EMERGENCY maintenance (unplanned,
 * failure-driven work) vs. PREVENTIVE (planned) — there's no real equivalent
 * of the old fake FailureEvent (a distinct downtime/incident record), so
 * downtime is approximated from the rapport's own labor hours rather than
 * invented from nothing.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FinancialDashboardService {

    private static final Set<MaintenanceType> FAILURE_TYPES = Set.of(MaintenanceType.CORRECTIVE, MaintenanceType.EMERGENCY);

    private final MaintenanceRapportRepository rapportRepository;
    private final MaintenanceRepository maintenanceRepository;
    private final AnnualBudgetRepository annualBudgetRepository;

    public FinancialDashboardResponse getDashboard() {
        log.info("Generating maintenance cost dashboard from real rapport/machine data");

        int currentYear = LocalDate.now().getYear();
        AnnualBudget budget = annualBudgetRepository.findByYear(currentYear).orElse(null);

        BigDecimal totalAllocated = budget != null ? budget.getTotalBudget() : BigDecimal.ZERO;
        BigDecimal totalSpent = budget != null ? budget.getSpentAmount() : BigDecimal.ZERO;
        BigDecimal totalRemaining = budget != null ? budget.getRemainingBudget() : BigDecimal.ZERO;
        double overallUtilization = budget != null && budget.getUtilizationPercentage() != null
                ? budget.getUtilizationPercentage().doubleValue()
                : 0.0;

        List<MaintenanceRapport> approvedRapports = rapportRepository.findByStatusOrderByCreatedDateDesc(RapportStatus.APPROVED);
        Map<Long, Maintenance> tasksById = resolveLinkedTasks(approvedRapports);

        return FinancialDashboardResponse.builder()
                .totalAllocated(totalAllocated)
                .totalSpent(totalSpent)
                .totalRemaining(totalRemaining)
                .overallUtilization(overallUtilization)
                .topCostMachines(buildTopCostMachines(approvedRapports, tasksById, 10))
                .alerts(buildBudgetAlerts(budget))
                .monthlyCosts(buildMonthlyCosts(approvedRapports, tasksById))
                .budgetsByDepartment(budget != null ? List.of(toBudgetResponse(budget)) : List.of())
                .build();
    }

    public List<FinancialDashboardResponse.TopCostMachine> getTopCostMachines(int limit) {
        List<MaintenanceRapport> approvedRapports = rapportRepository.findByStatusOrderByCreatedDateDesc(RapportStatus.APPROVED);
        return buildTopCostMachines(approvedRapports, resolveLinkedTasks(approvedRapports), limit);
    }

    public FailureReportResponse getFailureReport() {
        log.info("Generating failure report (corrective/emergency maintenance)");

        List<MaintenanceRapport> approvedRapports = rapportRepository.findByStatusOrderByCreatedDateDesc(RapportStatus.APPROVED);
        Map<Long, Maintenance> tasksById = resolveLinkedTasks(approvedRapports);
        List<MaintenanceRapport> failureRapports = approvedRapports.stream()
                .filter(r -> isFailureDriven(r, tasksById))
                .toList();

        BigDecimal totalCost = failureRapports.stream()
                .map(MaintenanceRapport::getTotalCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        double totalDowntime = failureRapports.stream()
                .mapToDouble(r -> r.getLaborHours() != null ? r.getLaborHours() : 0.0)
                .sum();

        List<FailureReportResponse.FailureDetail> details = failureRapports.stream()
                .map(r -> FailureReportResponse.FailureDetail.builder()
                        .id(r.getId())
                        .machineId(r.getMachineId())
                        .machineName(r.getMachineName())
                        .failureType(taskType(r, tasksById).name())
                        .downtimeHours(r.getLaborHours())
                        .cost(r.getTotalCost())
                        .occurredAt(r.getCreatedDate())
                        .build())
                .sorted(Comparator.comparing(FailureReportResponse.FailureDetail::getOccurredAt).reversed())
                .toList();

        return FailureReportResponse.builder()
                .totalFailures((long) failureRapports.size())
                .totalCost(totalCost)
                .totalDowntimeHours(totalDowntime)
                .failures(details)
                .build();
    }

    public Map<String, Object> getFailureSummary() {
        log.info("Generating failure summary");

        List<MaintenanceRapport> approvedRapports = rapportRepository.findByStatusOrderByCreatedDateDesc(RapportStatus.APPROVED);
        Map<Long, Maintenance> tasksById = resolveLinkedTasks(approvedRapports);
        List<MaintenanceRapport> failureRapports = approvedRapports.stream()
                .filter(r -> isFailureDriven(r, tasksById))
                .toList();

        BigDecimal totalCost = failureRapports.stream()
                .map(MaintenanceRapport::getTotalCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        double totalDowntime = failureRapports.stream()
                .mapToDouble(r -> r.getLaborHours() != null ? r.getLaborHours() : 0.0)
                .sum();

        BigDecimal averageCost = failureRapports.isEmpty()
                ? BigDecimal.ZERO
                : totalCost.divide(BigDecimal.valueOf(failureRapports.size()), 4, RoundingMode.HALF_UP);

        Map<String, Long> failuresByType = failureRapports.stream()
                .collect(Collectors.groupingBy(r -> taskType(r, tasksById).name(), Collectors.counting()));

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalFailures", failureRapports.size());
        summary.put("totalCost", totalCost);
        summary.put("averageCostPerFailure", averageCost);
        summary.put("totalDowntimeHours", totalDowntime);
        summary.put("failuresByType", failuresByType);
        summary.put("generatedAt", new Date());

        return summary;
    }

    public MaintenanceReportResponse getMaintenanceReport() {
        log.info("Generating maintenance report from approved rapports");

        List<MaintenanceRapport> approvedRapports = rapportRepository.findByStatusOrderByCreatedDateDesc(RapportStatus.APPROVED);
        Map<Long, Maintenance> tasksById = resolveLinkedTasks(approvedRapports);

        BigDecimal totalLaborCost = approvedRapports.stream()
                .map(MaintenanceRapport::getLaborCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPartsCost = approvedRapports.stream()
                .map(MaintenanceRapport::getPartsCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<MaintenanceReportResponse.MaintenanceDetail> details = approvedRapports.stream()
                .map(r -> {
                    Maintenance task = tasksById.get(r.getTaskId());
                    return MaintenanceReportResponse.MaintenanceDetail.builder()
                            .id(r.getId())
                            .machineId(r.getMachineId())
                            .machineName(r.getMachineName())
                            .type(taskType(r, tasksById).name())
                            .status("APPROVED")
                            .durationHours(r.getLaborHours())
                            .laborCost(r.getLaborCost())
                            .partsCost(r.getPartsCost())
                            .totalCost(r.getTotalCost())
                            .scheduledDate(task != null && task.getScheduledDate() != null
                                    ? task.getScheduledDate() : r.getCreatedDate())
                            .build();
                })
                .toList();

        return MaintenanceReportResponse.builder()
                .totalActions((long) approvedRapports.size())
                .totalCost(totalLaborCost.add(totalPartsCost))
                .totalLaborCost(totalLaborCost)
                .totalPartsCost(totalPartsCost)
                .actions(details)
                .build();
    }

    // ==================== HELPERS ====================

    private Map<Long, Maintenance> resolveLinkedTasks(List<MaintenanceRapport> rapports) {
        List<Long> taskIds = rapports.stream()
                .map(MaintenanceRapport::getTaskId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        if (taskIds.isEmpty()) {
            return Map.of();
        }
        return maintenanceRepository.findAllById(taskIds).stream()
                .collect(Collectors.toMap(Maintenance::getId, m -> m));
    }

    private MaintenanceType taskType(MaintenanceRapport rapport, Map<Long, Maintenance> tasksById) {
        Maintenance task = tasksById.get(rapport.getTaskId());
        return task != null && task.getType() != null ? task.getType() : MaintenanceType.CORRECTIVE;
    }

    private boolean isFailureDriven(MaintenanceRapport rapport, Map<Long, Maintenance> tasksById) {
        return FAILURE_TYPES.contains(taskType(rapport, tasksById));
    }

    private List<FinancialDashboardResponse.TopCostMachine> buildTopCostMachines(
            List<MaintenanceRapport> rapports, Map<Long, Maintenance> tasksById, int limit) {

        Map<Long, List<MaintenanceRapport>> byMachine = rapports.stream()
                .filter(r -> r.getMachineId() != null)
                .collect(Collectors.groupingBy(MaintenanceRapport::getMachineId));

        return byMachine.entrySet().stream()
                .map(entry -> {
                    List<MaintenanceRapport> machineRapports = entry.getValue();
                    BigDecimal maintenanceCost = machineRapports.stream()
                            .filter(r -> !isFailureDriven(r, tasksById))
                            .map(MaintenanceRapport::getTotalCost)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal failureCost = machineRapports.stream()
                            .filter(r -> isFailureDriven(r, tasksById))
                            .map(MaintenanceRapport::getTotalCost)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    return FinancialDashboardResponse.TopCostMachine.builder()
                            .machineId(entry.getKey())
                            .machineName(machineRapports.get(0).getMachineName())
                            .maintenanceCost(maintenanceCost)
                            .failureCost(failureCost)
                            .totalCost(maintenanceCost.add(failureCost))
                            .build();
                })
                .sorted(Comparator.comparing(FinancialDashboardResponse.TopCostMachine::getTotalCost).reversed())
                .limit(limit)
                .toList();
    }

    private List<FinancialDashboardResponse.BudgetAlert> buildBudgetAlerts(AnnualBudget budget) {
        if (budget == null || budget.getTotalBudget() == null || budget.getTotalBudget().compareTo(BigDecimal.ZERO) <= 0) {
            return List.of();
        }

        List<FinancialDashboardResponse.BudgetAlert> alerts = new ArrayList<>();
        BigDecimal remaining = budget.getRemainingBudget() != null ? budget.getRemainingBudget() : BigDecimal.ZERO;

        if (remaining.compareTo(BigDecimal.ZERO) < 0) {
            alerts.add(FinancialDashboardResponse.BudgetAlert.builder()
                    .department("Maintenance")
                    .period(String.valueOf(budget.getYear()))
                    .alertType("OVER_BUDGET")
                    .message("Annual budget exceeded by " + remaining.abs())
                    .amount(remaining.abs())
                    .build());
        } else {
            double utilization = budget.getUtilizationPercentage() != null ? budget.getUtilizationPercentage().doubleValue() : 0.0;
            if (utilization > 90.0) {
                alerts.add(FinancialDashboardResponse.BudgetAlert.builder()
                        .department("Maintenance")
                        .period(String.valueOf(budget.getYear()))
                        .alertType("NEAR_LIMIT")
                        .message(String.format("Budget %.1f%% utilized", utilization))
                        .amount(remaining)
                        .build());
            }
        }
        return alerts;
    }

    private List<FinancialDashboardResponse.MonthlyCost> buildMonthlyCosts(
            List<MaintenanceRapport> rapports, Map<Long, Maintenance> tasksById) {

        LocalDateTime cutoff = LocalDateTime.now().minusMonths(11).withDayOfMonth(1).toLocalDate().atStartOfDay();

        Map<String, List<MaintenanceRapport>> byMonth = rapports.stream()
                .filter(r -> r.getCreatedDate() != null && r.getCreatedDate().isAfter(cutoff))
                .collect(Collectors.groupingBy(r -> monthKey(r.getCreatedDate()), LinkedHashMap::new, Collectors.toList()));

        return byMonth.entrySet().stream()
                .map(entry -> {
                    BigDecimal maintenanceCost = entry.getValue().stream()
                            .filter(r -> !isFailureDriven(r, tasksById))
                            .map(MaintenanceRapport::getTotalCost)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal failureCost = entry.getValue().stream()
                            .filter(r -> isFailureDriven(r, tasksById))
                            .map(MaintenanceRapport::getTotalCost)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    return FinancialDashboardResponse.MonthlyCost.builder()
                            .month(entry.getKey())
                            .maintenanceCost(maintenanceCost)
                            .failureCost(failureCost)
                            .totalCost(maintenanceCost.add(failureCost))
                            .build();
                })
                .sorted(Comparator.comparing(FinancialDashboardResponse.MonthlyCost::getMonth))
                .toList();
    }

    private String monthKey(LocalDateTime date) {
        return date.getYear() + "-" + date.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
    }

    private BudgetResponse toBudgetResponse(AnnualBudget budget) {
        return BudgetResponse.builder()
                .id(budget.getId())
                .department("All Departments")
                .period(String.valueOf(budget.getYear()))
                .allocatedAmount(budget.getTotalBudget())
                .spentAmount(budget.getSpentAmount())
                .remainingAmount(budget.getRemainingBudget())
                .utilizationPercentage(budget.getUtilizationPercentage() != null ? budget.getUtilizationPercentage().doubleValue() : 0.0)
                .isOverBudget(budget.getRemainingBudget() != null && budget.getRemainingBudget().compareTo(BigDecimal.ZERO) < 0)
                .build();
    }
}
