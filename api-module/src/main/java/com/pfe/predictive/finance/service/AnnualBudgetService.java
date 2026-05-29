package com.pfe.predictive.finance.service;

import com.pfe.predictive.core.entity.finance.AnnualBudget;
import com.pfe.predictive.data.repository.finance.AnnualBudgetRepository;
import com.pfe.predictive.finance.dto.BudgetRequest;
import com.pfe.predictive.finance.dto.BudgetResponse;
import com.pfe.predictive.finance.mapper.AnnualBudgetMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AnnualBudgetService {

    private final AnnualBudgetRepository budgetRepository;
    private final AnnualBudgetMapper budgetMapper;

    // ─────────────────────────────────────────────────────────────────────────
    // CREATE
    // ─────────────────────────────────────────────────────────────────────────

    public BudgetResponse createBudget(BudgetRequest request, String createdBy) {
        log.info("Creating budget for year {} by {}", request.getYear(), createdBy);

        if (budgetRepository.findByYear(request.getYear()).isPresent()) {
            throw new IllegalStateException(
                    "A budget already exists for year " + request.getYear());
        }

        BigDecimal remaining = request.getTotalBudget();
        BigDecimal utilization = BigDecimal.ZERO;

        AnnualBudget budget = AnnualBudget.builder()
                .year(request.getYear())
                .totalBudget(request.getTotalBudget())
                .spentAmount(BigDecimal.ZERO)
                .remainingBudget(remaining)
                .utilizationPercentage(utilization)
                .notes(request.getNotes())
                .createdBy(createdBy)
                .build();

        AnnualBudget saved = budgetRepository.save(budget);
        log.info("Budget created — id={}, year={}, total={}", saved.getId(), saved.getYear(), saved.getTotalBudget());
        return budgetMapper.toResponse(saved);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // READ
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<BudgetResponse> getAllBudgets() {
        log.info("Fetching all annual budgets");
        return budgetRepository.findAll()
                .stream()
                .map(budgetMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BudgetResponse getBudgetByYear(Integer year) {
        log.info("Fetching budget for year {}", year);
        return budgetRepository.findByYear(year)
                .map(budgetMapper::toResponse)
                .orElseThrow(() -> new EntityNotFoundException(
                        "No budget found for year " + year));
    }

    @Transactional(readOnly = true)
    public BudgetResponse getBudgetById(Long id) {
        log.info("Fetching budget id={}", id);
        return budgetRepository.findById(id)
                .map(budgetMapper::toResponse)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Budget not found with id " + id));
    }

    @Transactional(readOnly = true)
    public Optional<BudgetResponse> getCurrentYearBudget() {
        int currentYear = LocalDate.now().getYear();
        log.info("Fetching current year ({}) budget", currentYear);
        return budgetRepository.findByYear(currentYear)
                .map(budgetMapper::toResponse);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UPDATE
    // ─────────────────────────────────────────────────────────────────────────

    public BudgetResponse updateBudget(Long id, BudgetRequest request) {
        log.info("Updating budget id={}", id);

        AnnualBudget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Budget not found with id " + id));

        // If year is changing, ensure no duplicate
        if (!budget.getYear().equals(request.getYear()) &&
                budgetRepository.findByYear(request.getYear()).isPresent()) {
            throw new IllegalStateException(
                    "A budget already exists for year " + request.getYear());
        }

        budget.setYear(request.getYear());
        budget.setTotalBudget(request.getTotalBudget());
        if (request.getNotes() != null) {
            budget.setNotes(request.getNotes());
        }
        recalculate(budget);

        AnnualBudget saved = budgetRepository.save(budget);
        log.info("Budget updated — id={}, year={}, total={}", saved.getId(), saved.getYear(), saved.getTotalBudget());
        return budgetMapper.toResponse(saved);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DELETE
    // ─────────────────────────────────────────────────────────────────────────

    public void deleteBudget(Long id) {
        log.info("Deleting budget id={}", id);
        AnnualBudget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Budget not found with id " + id));
        budgetRepository.delete(budget);
        log.info("Budget deleted — id={}", id);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // INTERNAL — called by ExpenseReportService on approval
    // ─────────────────────────────────────────────────────────────────────────

    public void recordApprovedExpense(BigDecimal amount, int year) {
        log.info("Recording approved expense of {} against year {} budget", amount, year);

        AnnualBudget budget = budgetRepository.findByYear(year)
                .orElseGet(() -> {
                    log.warn("No budget found for year {}; creating a zero-budget placeholder", year);
                    return budgetRepository.save(AnnualBudget.builder()
                            .year(year)
                            .totalBudget(BigDecimal.ZERO)
                            .spentAmount(BigDecimal.ZERO)
                            .remainingBudget(BigDecimal.ZERO)
                            .utilizationPercentage(BigDecimal.ZERO)
                            .build());
                });

        budget.setSpentAmount(budget.getSpentAmount().add(amount));
        recalculate(budget);
        budgetRepository.save(budget);
        log.info("Budget updated after approval — year={}, spent={}, remaining={}",
                year, budget.getSpentAmount(), budget.getRemainingBudget());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private void recalculate(AnnualBudget budget) {
        BigDecimal total = budget.getTotalBudget();
        BigDecimal spent = budget.getSpentAmount();

        budget.setRemainingBudget(total.subtract(spent));

        if (total.compareTo(BigDecimal.ZERO) > 0) {
            budget.setUtilizationPercentage(
                    spent.divide(total, 4, RoundingMode.HALF_UP)
                         .multiply(BigDecimal.valueOf(100))
                         .setScale(2, RoundingMode.HALF_UP));
        } else {
            budget.setUtilizationPercentage(BigDecimal.ZERO);
        }
    }
}
