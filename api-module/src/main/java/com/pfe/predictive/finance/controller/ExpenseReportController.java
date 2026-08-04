package com.pfe.predictive.finance.controller;

import com.pfe.predictive.core.entity.finance.ExpenseCategory;
import com.pfe.predictive.finance.dto.ApproveExpenseRequest;
import com.pfe.predictive.finance.dto.ExpenseReportRequest;
import com.pfe.predictive.finance.dto.ExpenseReportResponse;
import com.pfe.predictive.finance.dto.ExpenseSummaryResponse;
import com.pfe.predictive.finance.dto.RejectExpenseRequest;
import com.pfe.predictive.finance.service.ExpenseReportService;
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
@RequestMapping("/api/v1/finance/expenses")
@RequiredArgsConstructor
@Tag(name = "Finance — Expense Reports", description = "Submit, review, and manage expense reports")
public class ExpenseReportController {

    private final ExpenseReportService expenseService;

    // ─────────────────────────────────────────────────────────────────────────
    // CREATE
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping
    @Operation(summary = "Submit a new expense report")
    public ResponseEntity<ExpenseReportResponse> createExpense(
            @Valid @RequestBody ExpenseReportRequest request,
            Authentication authentication) {

        String username = authentication.getName();
        String fullName = resolveFullName(authentication);
        log.info("POST /finance/expenses — user={}", username);

        ExpenseReportResponse response = expenseService.createExpense(request, username, fullName);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // READ
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/mine")
    @Operation(summary = "Get my submitted expense reports")
    public ResponseEntity<List<ExpenseReportResponse>> getMyExpenses(Authentication authentication) {
        String username = authentication.getName();
        log.info("GET /finance/expenses/mine — user={}", username);
        return ResponseEntity.ok(expenseService.getMyExpenses(username));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('FINANCE_MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Get all expense reports (finance managers only)")
    public ResponseEntity<List<ExpenseReportResponse>> getAllExpenses() {
        log.info("GET /finance/expenses");
        return ResponseEntity.ok(expenseService.getAllExpenses());
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('FINANCE_MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Get all pending expense reports")
    public ResponseEntity<List<ExpenseReportResponse>> getPendingExpenses() {
        log.info("GET /finance/expenses/pending");
        return ResponseEntity.ok(expenseService.getPendingExpenses());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('FINANCE_MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Get expense report by ID (submitters use /mine for their own reports)")
    public ResponseEntity<ExpenseReportResponse> getExpenseById(@PathVariable Long id) {
        log.info("GET /finance/expenses/{}", id);
        return ResponseEntity.ok(expenseService.getExpenseById(id));
    }

    @GetMapping("/category/{category}")
    @PreAuthorize("hasAnyRole('FINANCE_MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Get expense reports by category")
    public ResponseEntity<List<ExpenseReportResponse>> getByCategory(
            @PathVariable ExpenseCategory category) {
        log.info("GET /finance/expenses/category/{}", category);
        return ResponseEntity.ok(expenseService.getExpensesByCategory(category));
    }

    @GetMapping("/machine/{machineId}")
    @PreAuthorize("hasAnyRole('FINANCE_MANAGER', 'MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Get expense reports linked to a machine")
    public ResponseEntity<List<ExpenseReportResponse>> getByMachine(@PathVariable Long machineId) {
        log.info("GET /finance/expenses/machine/{}", machineId);
        return ResponseEntity.ok(expenseService.getExpensesByMachine(machineId));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UPDATE / DELETE
    // ─────────────────────────────────────────────────────────────────────────

    @PutMapping("/{id}")
    @Operation(summary = "Update a pending expense report (submitter only)")
    public ResponseEntity<ExpenseReportResponse> updateExpense(
            @PathVariable Long id,
            @Valid @RequestBody ExpenseReportRequest request,
            Authentication authentication) {

        String username = authentication.getName();
        log.info("PUT /finance/expenses/{} — user={}", id, username);
        return ResponseEntity.ok(expenseService.updateExpense(id, request, username));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a pending expense report (submitter only)")
    public ResponseEntity<Void> deleteExpense(
            @PathVariable Long id,
            Authentication authentication) {

        String username = authentication.getName();
        log.info("DELETE /finance/expenses/{} — user={}", id, username);
        expenseService.deleteExpense(id, username);
        return ResponseEntity.noContent().build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // APPROVE / REJECT
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('FINANCE_MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Approve a pending expense report")
    public ResponseEntity<ExpenseReportResponse> approveExpense(
            @PathVariable Long id,
            @RequestBody(required = false) ApproveExpenseRequest request,
            Authentication authentication) {

        String username = authentication.getName();
        log.info("POST /finance/expenses/{}/approve — reviewer={}", id, username);

        ApproveExpenseRequest safeRequest = request != null ? request : new ApproveExpenseRequest();
        return ResponseEntity.ok(expenseService.approveExpense(id, safeRequest, username));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('FINANCE_MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Reject a pending expense report")
    public ResponseEntity<ExpenseReportResponse> rejectExpense(
            @PathVariable Long id,
            @Valid @RequestBody RejectExpenseRequest request,
            Authentication authentication) {

        String username = authentication.getName();
        log.info("POST /finance/expenses/{}/reject — reviewer={}", id, username);
        return ResponseEntity.ok(expenseService.rejectExpense(id, request, username));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SUMMARY
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('FINANCE_MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Get expense summary by month and year")
    public ResponseEntity<ExpenseSummaryResponse> getExpenseSummary(
            @RequestParam int year,
            @RequestParam int month,
            Authentication authentication) {

        log.info("Fetching expense summary for {}/{} requested by {}", month, year, authentication.getName());
        return ResponseEntity.ok(expenseService.getExpenseSummary(year, month));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Attempts to extract a display name from the authentication principal.
     * Falls back to the username if no name attribute is available.
     */
    private String resolveFullName(Authentication authentication) {
        if (authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails ud) {
            return ud.getUsername();
        }
        return authentication.getName();
    }
}
