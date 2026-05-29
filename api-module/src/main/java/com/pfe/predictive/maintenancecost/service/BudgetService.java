package com.pfe.predictive.maintenancecost.service;

import com.pfe.predictive.maintenancecost.dto.BudgetRequest;
import com.pfe.predictive.maintenancecost.dto.BudgetResponse;
import com.pfe.predictive.maintenancecost.entity.MaintenanceBudget;
import com.pfe.predictive.maintenancecost.repository.MaintenanceBudgetRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Budget Service - Core financial management
 * Handles budget creation, updates, and automatic cost tracking
 * 
 * @author Finance Module
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class BudgetService {

    private final MaintenanceBudgetRepository budgetRepository;

    /**
     * Create new budget for department and period
     * @param request Budget creation request
     * @return Created budget
     */
    public BudgetResponse createBudget(BudgetRequest request) {
        log.info("Creating budget - Department: {}, Period: {}", request.getDepartment(), request.getPeriod());

        // Check if budget already exists
        budgetRepository.findByDepartmentAndPeriod(request.getDepartment(), request.getPeriod())
            .ifPresent(existing -> {
                throw new IllegalArgumentException(
                    "Budget already exists for " + request.getDepartment() + " / " + request.getPeriod()
                );
            });

        MaintenanceBudget budget = MaintenanceBudget.builder()
            .department(request.getDepartment())
            .period(request.getPeriod())
            .allocatedAmount(request.getAllocatedAmount())
            .spentAmount(BigDecimal.ZERO)
            .remainingAmount(request.getAllocatedAmount()) // Will be computed by @PrePersist
            .build();

        MaintenanceBudget saved = budgetRepository.save(budget);
        log.info("Budget created - ID: {}, Allocated: {}", saved.getId(), saved.getAllocatedAmount());
        
        return toResponse(saved);
    }

    /**
     * Get budget by department and period
     * @param department Department name
     * @param period Period (e.g., "2026-Q2")
     * @return Budget
     */
    @Transactional(readOnly = true)
    public BudgetResponse getBudget(String department, String period) {
        log.debug("Fetching budget - Department: {}, Period: {}", department, period);
        
        MaintenanceBudget budget = budgetRepository.findByDepartmentAndPeriod(department, period)
            .orElseThrow(() -> new EntityNotFoundException(
                "Budget not found for " + department + " / " + period
            ));
        
        return toResponse(budget);
    }

    /**
     * Get all budgets
     * @return List of all budgets
     */
    @Transactional(readOnly = true)
    public List<BudgetResponse> getAllBudgets() {
        log.debug("Fetching all budgets");
        return budgetRepository.findAll()
            .stream()
            .map(this::toResponse)
            .toList();
    }

    /**
     * Update budget allocation
     * @param id Budget ID
     * @param request Update request
     * @return Updated budget
     */
    public BudgetResponse updateBudget(Long id, BudgetRequest request) {
        log.info("Updating budget - ID: {}", id);

        MaintenanceBudget budget = budgetRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Budget not found: " + id));

        if (request.getAllocatedAmount() != null) {
            budget.setAllocatedAmount(request.getAllocatedAmount());
        }
        if (request.getDepartment() != null) {
            budget.setDepartment(request.getDepartment());
        }
        if (request.getPeriod() != null) {
            budget.setPeriod(request.getPeriod());
        }

        MaintenanceBudget updated = budgetRepository.save(budget);
        log.info("Budget updated - ID: {}, New allocated: {}", id, updated.getAllocatedAmount());
        
        return toResponse(updated);
    }

    /**
     * Add cost to budget (CRITICAL METHOD)
     * This is called automatically when maintenance is completed or failure occurs
     * 
     * @param department Department name
     * @param period Period
     * @param cost Cost to add
     */
    public void addCostToBudget(String department, String period, BigDecimal cost) {
        log.info("Adding cost to budget - Department: {}, Period: {}, Cost: {}", 
                 department, period, cost);

        MaintenanceBudget budget = budgetRepository.findByDepartmentAndPeriod(department, period)
            .orElseThrow(() -> new EntityNotFoundException(
                "Budget not found for " + department + " / " + period + 
                ". Please create budget first."
            ));

        BigDecimal currentSpent = budget.getSpentAmount() != null 
            ? budget.getSpentAmount() 
            : BigDecimal.ZERO;
        
        budget.setSpentAmount(currentSpent.add(cost));
        // remainingAmount will be auto-computed by @PreUpdate
        
        budgetRepository.save(budget);
        
        log.info("Budget updated - Spent: {}, Remaining: {}", 
                 budget.getSpentAmount(), budget.getRemainingAmount());

        // Alert if budget exceeded
        if (budget.getRemainingAmount().compareTo(BigDecimal.ZERO) < 0) {
            log.warn("⚠️ BUDGET EXCEEDED - Department: {}, Period: {}, Overspent: {}", 
                     department, period, budget.getRemainingAmount().abs());
        }
    }

    /**
     * Check if budget has sufficient funds
     * @param department Department
     * @param period Period
     * @param requiredAmount Amount needed
     * @return true if sufficient
     */
    @Transactional(readOnly = true)
    public boolean hasSufficientBudget(String department, String period, BigDecimal requiredAmount) {
        MaintenanceBudget budget = budgetRepository.findByDepartmentAndPeriod(department, period)
            .orElse(null);
        
        if (budget == null) {
            log.warn("No budget found for {} / {}", department, period);
            return false;
        }

        return budget.getRemainingAmount().compareTo(requiredAmount) >= 0;
    }

    /**
     * Get budget utilization percentage
     * @param department Department
     * @param period Period
     * @return Percentage (0-100+)
     */
    @Transactional(readOnly = true)
    public double getBudgetUtilization(String department, String period) {
        MaintenanceBudget budget = budgetRepository.findByDepartmentAndPeriod(department, period)
            .orElseThrow(() -> new EntityNotFoundException(
                "Budget not found for " + department + " / " + period
            ));

        if (budget.getAllocatedAmount().compareTo(BigDecimal.ZERO) == 0) {
            return 0.0;
        }

        return budget.getSpentAmount()
            .divide(budget.getAllocatedAmount(), 4, java.math.RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100))
            .doubleValue();
    }

    /**
     * Delete budget
     * @param id Budget ID
     */
    public void deleteBudget(Long id) {
        log.info("Deleting budget - ID: {}", id);
        
        MaintenanceBudget budget = budgetRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Budget not found: " + id));

        if (budget.getSpentAmount().compareTo(BigDecimal.ZERO) > 0) {
            log.warn("Deleting budget with spent amount: {}", budget.getSpentAmount());
        }

        budgetRepository.delete(budget);
        log.info("Budget deleted - ID: {}", id);
    }

    // ==================== HELPER METHODS ====================

    private BudgetResponse toResponse(MaintenanceBudget budget) {
        return BudgetResponse.builder()
            .id(budget.getId())
            .department(budget.getDepartment())
            .period(budget.getPeriod())
            .allocatedAmount(budget.getAllocatedAmount())
            .spentAmount(budget.getSpentAmount())
            .remainingAmount(budget.getRemainingAmount())
            .utilizationPercentage(calculateUtilization(budget))
            .isOverBudget(budget.getRemainingAmount().compareTo(BigDecimal.ZERO) < 0)
            .build();
    }

    private double calculateUtilization(MaintenanceBudget budget) {
        if (budget.getAllocatedAmount().compareTo(BigDecimal.ZERO) == 0) {
            return 0.0;
        }
        return budget.getSpentAmount()
            .divide(budget.getAllocatedAmount(), 4, java.math.RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100))
            .doubleValue();
    }
}
