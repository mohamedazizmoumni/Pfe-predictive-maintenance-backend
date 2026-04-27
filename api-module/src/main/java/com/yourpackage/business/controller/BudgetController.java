package com.yourpackage.business.controller;

import com.pfe.predictive.maintenancecost.entity.MaintenanceBudget;
import com.yourpackage.business.dto.MaintenanceBudgetDTO;
import com.yourpackage.business.dto.RegisterExpenseRequestDTO;
import com.yourpackage.business.service.BudgetService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@Validated
@RequestMapping("/api/v1/budgets")
public class BudgetController {

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @GetMapping
    public ResponseEntity<List<MaintenanceBudgetDTO>> getAllBudgets() {
        return ResponseEntity.ok(budgetService.findAllBudgets());
    }

    @GetMapping("/id/{budgetId}")
    public ResponseEntity<MaintenanceBudgetDTO> getBudgetById(@PathVariable Long budgetId) {
        return ResponseEntity.ok(budgetService.findBudgetById(budgetId));
    }

    @PostMapping
    public ResponseEntity<MaintenanceBudgetDTO> createBudget(@RequestBody @Valid MaintenanceBudget body) {
        MaintenanceBudgetDTO created = budgetService.createBudget(body);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{budgetId}")
    public ResponseEntity<MaintenanceBudgetDTO> updateBudget(@PathVariable Long budgetId,
                                                             @RequestBody @Valid MaintenanceBudget body) {
        return ResponseEntity.ok(budgetService.updateBudget(budgetId, body));
    }

    @DeleteMapping("/{budgetId}")
    public ResponseEntity<Void> deleteBudget(@PathVariable Long budgetId) {
        budgetService.deleteBudget(budgetId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{department}/{period}")
    public ResponseEntity<MaintenanceBudgetDTO> getBudgetStatus(@PathVariable String department,
                                                                @PathVariable String period) {
        MaintenanceBudgetDTO status = budgetService.getBudgetStatus(department, period);
        return ResponseEntity.ok(status);
    }

    @PostMapping("/{budgetId}/expense")
    public ResponseEntity<MaintenanceBudgetDTO> registerExpense(@PathVariable Long budgetId,
                                                                @Valid @RequestBody RegisterExpenseRequestDTO body) {
        MaintenanceBudgetDTO updated = budgetService.registerExpense(budgetId, body.getAmount());
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/{budgetId}/alert")
    public ResponseEntity<Map<String, Boolean>> getBudgetAlert(@PathVariable Long budgetId) {
        boolean result = budgetService.checkBudgetAlert(budgetId);
        return ResponseEntity.ok(Map.of("alertTriggered", result));
    }

    @GetMapping("/{budgetId}/canAfford")
    public ResponseEntity<Map<String, Boolean>> canAfford(@PathVariable Long budgetId,
                                                           @RequestParam("actionCost") @NotNull BigDecimal actionCost) {
        boolean result = budgetService.canAffordAction(budgetId, actionCost);
        return ResponseEntity.ok(Map.of("canAfford", result));
    }
}
