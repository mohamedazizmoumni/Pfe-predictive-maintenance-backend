package com.pfe.predictive.finance.service;

import com.pfe.predictive.core.entity.finance.ExpenseCategory;
import com.pfe.predictive.core.entity.finance.ExpenseReport;
import com.pfe.predictive.core.entity.finance.ExpenseStatus;
import com.pfe.predictive.core.entity.finance.MaintenanceRapport;
import com.pfe.predictive.core.entity.finance.RapportStatus;
import com.pfe.predictive.data.repository.finance.AnnualBudgetRepository;
import com.pfe.predictive.data.repository.finance.ExpenseReportRepository;
import com.pfe.predictive.data.repository.finance.MaintenanceRapportRepository;
import com.pfe.predictive.finance.dto.FinanceDashboardResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FinanceDashboardService {

    private final ExpenseReportRepository expenseRepository;
    private final AnnualBudgetRepository budgetRepository;
    private final MaintenanceRapportRepository rapportRepository;

    public FinanceDashboardResponse getDashboard() {
        int currentYear = LocalDate.now().getYear();
        log.info("Building finance dashboard for year {}", currentYear);

        // ── Budget ────────────────────────────────────────────────────────────
        var budgetOpt = budgetRepository.findByYear(currentYear);
        boolean budgetExists = budgetOpt.isPresent();

        BigDecimal totalBudget          = budgetOpt.map(b -> b.getTotalBudget()).orElse(BigDecimal.ZERO);
        BigDecimal spentAmount          = budgetOpt.map(b -> b.getSpentAmount()).orElse(BigDecimal.ZERO);
        BigDecimal remainingBudget      = budgetOpt.map(b -> b.getRemainingBudget()).orElse(BigDecimal.ZERO);
        BigDecimal utilizationPct       = budgetOpt.map(b -> b.getUtilizationPercentage()).orElse(BigDecimal.ZERO);

        // ── Expense counts ────────────────────────────────────────────────────
        long pendingCount  = expenseRepository.countByStatus(ExpenseStatus.PENDING);
        long approvedCount = expenseRepository.countByStatus(ExpenseStatus.APPROVED);
        long rejectedCount = expenseRepository.countByStatus(ExpenseStatus.REJECTED);
        long totalReports  = expenseRepository.count();

        // ── Expense amounts ───────────────────────────────────────────────────
        List<ExpenseReport> allExpenses = expenseRepository.findAll();

        BigDecimal pendingAmount = allExpenses.stream()
                .filter(e -> e.getStatus() == ExpenseStatus.PENDING)
                .map(ExpenseReport::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Maintenance rapports awaiting this finance manager's own sign-off are
        // just as much "pending approval" as an expense report — without this
        // a rapport sitting in the Rapport Approvals queue never showed up here.
        List<MaintenanceRapport> pendingFinanceRapports =
                rapportRepository.findByStatusOrderByCreatedDateDesc(RapportStatus.PENDING_FINANCE_APPROVAL);

        pendingCount += pendingFinanceRapports.size();
        pendingAmount = pendingAmount.add(pendingFinanceRapports.stream()
                .map(MaintenanceRapport::getTotalCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        BigDecimal approvedAmount = allExpenses.stream()
                .filter(e -> e.getStatus() == ExpenseStatus.APPROVED)
                .map(ExpenseReport::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // ── Expenses by category (approved only, all 5 categories present) ────
        Map<String, BigDecimal> byCategory = new LinkedHashMap<>();
        for (ExpenseCategory cat : ExpenseCategory.values()) {
            byCategory.put(cat.name(), BigDecimal.ZERO);
        }
        allExpenses.stream()
                .filter(e -> e.getStatus() == ExpenseStatus.APPROVED && e.getCategory() != null)
                .forEach(e -> byCategory.merge(
                        e.getCategory().name(),
                        e.getAmount(),
                        BigDecimal::add));

        // Approved rapports never produce an ExpenseReport, so their cost was
        // invisible here too — attribute it at the same granularity the rapport
        // itself tracks (parts vs. labor) rather than lumping it all under a
        // single "Maintenance" bucket, since that's the breakdown a technician
        // actually itemized on the rapport.
        List<MaintenanceRapport> approvedRapports =
                rapportRepository.findByStatusOrderByCreatedDateDesc(RapportStatus.APPROVED);
        approvedRapports.forEach(r -> {
            byCategory.merge(ExpenseCategory.PARTS.name(), r.getPartsCost(), BigDecimal::add);
            byCategory.merge(ExpenseCategory.LABOR.name(), r.getLaborCost(), BigDecimal::add);
        });

        return FinanceDashboardResponse.builder()
                .currentYear(currentYear)
                .totalBudget(totalBudget)
                .spentAmount(spentAmount)
                .remainingBudget(remainingBudget)
                .utilizationPercentage(utilizationPct)
                .pendingCount(pendingCount)
                .approvedCount(approvedCount)
                .rejectedCount(rejectedCount)
                .pendingAmount(pendingAmount)
                .approvedAmount(approvedAmount)
                .expensesByCategory(byCategory)
                .totalExpenseReports(totalReports)
                .budgetExists(budgetExists)
                .build();
    }
}
