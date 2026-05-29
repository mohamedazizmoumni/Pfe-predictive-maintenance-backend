package com.pfe.predictive.finance.controller;

import com.pfe.predictive.finance.dto.BudgetRequest;
import com.pfe.predictive.finance.dto.BudgetResponse;
import com.pfe.predictive.finance.service.AnnualBudgetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/finance/budget")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('FINANCE_MANAGER', 'ADMIN', 'SUPER_ADMIN')")
@Tag(name = "Finance — Annual Budget", description = "Manage annual maintenance budgets")
public class AnnualBudgetController {

    private final AnnualBudgetService budgetService;

    @PostMapping
    @Operation(summary = "Create a new annual budget")
    public ResponseEntity<BudgetResponse> createBudget(
            @Valid @RequestBody BudgetRequest request,
            Authentication authentication) {

        String username = authentication.getName();
        log.info("POST /finance/budget — year={}, user={}", request.getYear(), username);
        BudgetResponse response = budgetService.createBudget(request, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Get all annual budgets")
    public ResponseEntity<List<BudgetResponse>> getAllBudgets() {
        log.info("GET /finance/budget");
        return ResponseEntity.ok(budgetService.getAllBudgets());
    }

    @GetMapping("/current")
    @Operation(summary = "Get the current year budget")
    public ResponseEntity<BudgetResponse> getCurrentYearBudget() {
        log.info("GET /finance/budget/current");
        return budgetService.getCurrentYearBudget()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @GetMapping("/year/{year}")
    @Operation(summary = "Get budget by year")
    public ResponseEntity<BudgetResponse> getBudgetByYear(@PathVariable Integer year) {
        log.info("GET /finance/budget/year/{}", year);
        return ResponseEntity.ok(budgetService.getBudgetByYear(year));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get budget by ID")
    public ResponseEntity<BudgetResponse> getBudgetById(@PathVariable Long id) {
        log.info("GET /finance/budget/{}", id);
        return ResponseEntity.ok(budgetService.getBudgetById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an annual budget")
    public ResponseEntity<BudgetResponse> updateBudget(
            @PathVariable Long id,
            @Valid @RequestBody BudgetRequest request) {

        log.info("PUT /finance/budget/{}", id);
        return ResponseEntity.ok(budgetService.updateBudget(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an annual budget")
    public ResponseEntity<Void> deleteBudget(@PathVariable Long id) {
        log.info("DELETE /finance/budget/{}", id);
        budgetService.deleteBudget(id);
        return ResponseEntity.noContent().build();
    }
}
