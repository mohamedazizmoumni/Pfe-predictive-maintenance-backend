package com.yourpackage.business.service;

import com.pfe.predictive.maintenancecost.entity.MaintenanceBudget;
import com.pfe.predictive.maintenancecost.repository.MaintenanceBudgetRepository;
import com.yourpackage.business.dto.MaintenanceBudgetDTO;
import com.yourpackage.business.exception.ResourceNotFoundException;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@Validated
public class BudgetService {

    private final MaintenanceBudgetRepository maintenanceBudgetRepository;

    public BudgetService(MaintenanceBudgetRepository maintenanceBudgetRepository) {
        this.maintenanceBudgetRepository = maintenanceBudgetRepository;
    }

    public MaintenanceBudgetDTO getBudgetStatus(@NotBlank String department, @NotBlank String period) {
        MaintenanceBudget budget = maintenanceBudgetRepository.findByDepartmentAndPeriod(department, period)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Budget not found for department " + department + " and period " + period
                ));
        return toDto(budget);
    }

    public List<MaintenanceBudgetDTO> findAllBudgets() {
        return maintenanceBudgetRepository.findAll().stream().map(this::toDto).toList();
    }

    public MaintenanceBudgetDTO findBudgetById(@NotNull Long budgetId) {
        MaintenanceBudget budget = maintenanceBudgetRepository.findById(budgetId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found with id " + budgetId));
        return toDto(budget);
    }

    public MaintenanceBudgetDTO createBudget(@NotNull MaintenanceBudget budget) {
        MaintenanceBudget saved = maintenanceBudgetRepository.save(budget);
        return toDto(saved);
    }

    public MaintenanceBudgetDTO updateBudget(@NotNull Long budgetId, @NotNull MaintenanceBudget payload) {
        MaintenanceBudget budget = maintenanceBudgetRepository.findById(budgetId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found with id " + budgetId));

        budget.setDepartment(payload.getDepartment());
        budget.setPeriod(payload.getPeriod());
        budget.setAllocatedAmount(payload.getAllocatedAmount());
        budget.setSpentAmount(payload.getSpentAmount());

        MaintenanceBudget saved = maintenanceBudgetRepository.save(budget);
        return toDto(saved);
    }

    public void deleteBudget(@NotNull Long budgetId) {
        MaintenanceBudget budget = maintenanceBudgetRepository.findById(budgetId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found with id " + budgetId));
        maintenanceBudgetRepository.delete(budget);
    }

    public MaintenanceBudgetDTO registerExpense(@NotNull Long budgetId, @NotNull @Positive BigDecimal amount) {
        MaintenanceBudget budget = maintenanceBudgetRepository.findById(budgetId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found with id " + budgetId));

        BigDecimal updatedSpent = budget.getSpentAmount().add(amount);
        budget.setSpentAmount(updatedSpent);
        budget.setRemainingAmount(budget.getAllocatedAmount().subtract(updatedSpent).setScale(4, RoundingMode.HALF_UP));

        MaintenanceBudget savedBudget = maintenanceBudgetRepository.save(budget);
        return toDto(savedBudget);
    }

    public boolean checkBudgetAlert(@NotNull Long budgetId) {
        MaintenanceBudget budget = maintenanceBudgetRepository.findById(budgetId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found with id " + budgetId));

        BigDecimal threshold = budget.getAllocatedAmount().multiply(BigDecimal.valueOf(0.80));
        return budget.getSpentAmount().compareTo(threshold) > 0;
    }

    public boolean canAffordAction(@NotNull Long budgetId, @NotNull @Positive BigDecimal actionCost) {
        MaintenanceBudget budget = maintenanceBudgetRepository.findById(budgetId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found with id " + budgetId));
        return budget.getRemainingAmount().compareTo(actionCost) >= 0;
    }

    private MaintenanceBudgetDTO toDto(MaintenanceBudget budget) {
        double percentageUsed = computePercentageUsed(budget.getSpentAmount(), budget.getAllocatedAmount());

        return MaintenanceBudgetDTO.builder()
                .budgetId(budget.getId())
                .department(budget.getDepartment())
                .period(budget.getPeriod())
                .allocatedAmount(budget.getAllocatedAmount())
                .spentAmount(budget.getSpentAmount())
                .remainingAmount(budget.getRemainingAmount())
                .percentageUsed(percentageUsed)
                .alertTriggered(percentageUsed > 80.0)
                .build();
    }

    private double computePercentageUsed(BigDecimal spentAmount, BigDecimal allocatedAmount) {
        if (allocatedAmount == null || allocatedAmount.compareTo(BigDecimal.ZERO) == 0) {
            return 0.0;
        }
        return spentAmount
                .multiply(BigDecimal.valueOf(100))
                .divide(allocatedAmount, 2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
