package com.pfe.predictive.maintenancecost.controller;

import com.pfe.predictive.maintenancecost.dto.BudgetRequest;
import com.pfe.predictive.maintenancecost.dto.BudgetResponse;
import com.pfe.predictive.maintenancecost.service.BudgetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Budget Controller
 * Manages maintenance budgets
 * 
 * Security: Only FINANCE_MANAGER and ADMIN can access
 * 
 * @author Finance Module
 * @version 1.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/maintenance/budgets")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('FINANCE_MANAGER', 'ADMIN', 'SUPER_ADMIN')")
public class BudgetController {

    private final BudgetService budgetService;

    /**
     * Create new budget
     * POST /api/v1/finance/budgets
     */
    @PostMapping
    public ResponseEntity<BudgetResponse> createBudget(@Valid @RequestBody BudgetRequest request) {
        log.info("📝 Creating budget - Department: {}, Period: {}", 
                 request.getDepartment(), request.getPeriod());
        
        BudgetResponse response = budgetService.createBudget(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get all budgets
     * GET /api/v1/finance/budgets
     */
    @GetMapping
    public ResponseEntity<List<BudgetResponse>> getAllBudgets() {
        log.info("📋 Fetching all budgets");
        List<BudgetResponse> budgets = budgetService.getAllBudgets();
        return ResponseEntity.ok(budgets);
    }

    /**
     * Get budget by department and period
     * GET /api/v1/finance/budgets/{department}/{period}
     */
    @GetMapping("/{department}/{period}")
    public ResponseEntity<BudgetResponse> getBudget(
            @PathVariable String department,
            @PathVariable String period) {
        
        log.info("🔍 Fetching budget - Department: {}, Period: {}", department, period);
        BudgetResponse budget = budgetService.getBudget(department, period);
        return ResponseEntity.ok(budget);
    }

    /**
     * Update budget
     * PUT /api/v1/finance/budgets/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<BudgetResponse> updateBudget(
            @PathVariable Long id,
            @Valid @RequestBody BudgetRequest request) {
        
        log.info("✏️ Updating budget - ID: {}", id);
        BudgetResponse response = budgetService.updateBudget(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete budget
     * DELETE /api/v1/finance/budgets/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBudget(@PathVariable Long id) {
        log.info("🗑️ Deleting budget - ID: {}", id);
        budgetService.deleteBudget(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get budget utilization percentage
     * GET /api/v1/finance/budgets/{department}/{period}/utilization
     */
    @GetMapping("/{department}/{period}/utilization")
    public ResponseEntity<Double> getBudgetUtilization(
            @PathVariable String department,
            @PathVariable String period) {
        
        log.info("📊 Fetching budget utilization - Department: {}, Period: {}", department, period);
        double utilization = budgetService.getBudgetUtilization(department, period);
        return ResponseEntity.ok(utilization);
    }
}
