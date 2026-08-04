package com.pfe.predictive.finance.service;

import com.pfe.predictive.audit.service.AuditEventService;
import com.pfe.predictive.core.entity.finance.ExpenseCategory;
import com.pfe.predictive.core.entity.finance.ExpenseReport;
import com.pfe.predictive.core.entity.finance.ExpenseStatus;
import com.pfe.predictive.data.repository.finance.ExpenseReportRepository;
import com.pfe.predictive.finance.dto.ApproveExpenseRequest;
import com.pfe.predictive.finance.dto.ExpenseReportRequest;
import com.pfe.predictive.finance.dto.ExpenseReportResponse;
import com.pfe.predictive.finance.dto.ExpenseSummaryResponse;
import com.pfe.predictive.finance.dto.RejectExpenseRequest;
import com.pfe.predictive.finance.mapper.ExpenseReportMapper;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers the expense-report approval workflow: only PENDING reports can be
 * mutated, only the submitter can edit/delete their own report, and
 * approval feeds the amount into the annual budget for that report's year.
 */
@ExtendWith(MockitoExtension.class)
class ExpenseReportServiceTest {

    @Mock
    private ExpenseReportRepository expenseRepository;

    @Mock
    private AnnualBudgetService budgetService;

    @Mock
    private AuditEventService auditEventService;

    private ExpenseReportService service;

    @BeforeEach
    void setUp() {
        service = new ExpenseReportService(expenseRepository, new ExpenseReportMapper(), budgetService, auditEventService);
    }

    private ExpenseReport expense(ExpenseStatus status, String submittedBy) {
        return ExpenseReport.builder()
                .id(1L)
                .title("New bearing")
                .amount(new BigDecimal("250.00"))
                .category(ExpenseCategory.PARTS)
                .submittedBy(submittedBy)
                .status(status)
                .createdDate(LocalDateTime.of(2026, 3, 15, 10, 0))
                .build();
    }

    // ------------------------------------------------------------------
    // approve / reject
    // ------------------------------------------------------------------

    @Test
    void approveExpenseUpdatesStatusAndFeedsBudget() {
        ExpenseReport expense = expense(ExpenseStatus.PENDING, "jane.tech");
        when(expenseRepository.findById(1L)).thenReturn(Optional.of(expense));
        when(expenseRepository.save(any(ExpenseReport.class))).thenAnswer(inv -> inv.getArgument(0));

        ExpenseReportResponse response = service.approveExpense(
                1L, ApproveExpenseRequest.builder().reviewNote("Looks good").build(), "finance.mgr");

        assertEquals("APPROVED", response.getStatus());
        assertEquals("finance.mgr", response.getReviewedBy());
        verify(budgetService).recordApprovedExpense(new BigDecimal("250.00"), 2026);
    }

    @Test
    void approveExpenseRejectsAlreadyDecidedReport() {
        ExpenseReport expense = expense(ExpenseStatus.APPROVED, "jane.tech");
        when(expenseRepository.findById(1L)).thenReturn(Optional.of(expense));

        assertThrows(IllegalStateException.class,
                () -> service.approveExpense(1L, ApproveExpenseRequest.builder().build(), "finance.mgr"));
        verify(budgetService, never()).recordApprovedExpense(any(), anyInt());
    }

    @Test
    void rejectExpenseStoresReasonAndDoesNotTouchBudget() {
        ExpenseReport expense = expense(ExpenseStatus.PENDING, "jane.tech");
        when(expenseRepository.findById(1L)).thenReturn(Optional.of(expense));
        when(expenseRepository.save(any(ExpenseReport.class))).thenAnswer(inv -> inv.getArgument(0));

        ExpenseReportResponse response = service.rejectExpense(
                1L, RejectExpenseRequest.builder().rejectionReason("Missing receipt").build(), "finance.mgr");

        assertEquals("REJECTED", response.getStatus());
        assertEquals("Missing receipt", response.getRejectionReason());
        verify(budgetService, never()).recordApprovedExpense(any(), anyInt());
    }

    @Test
    void approveExpenseThrowsWhenMissing() {
        when(expenseRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> service.approveExpense(404L, ApproveExpenseRequest.builder().build(), "finance.mgr"));
    }

    // ------------------------------------------------------------------
    // updateExpense / deleteExpense: ownership + pending-only guards
    // ------------------------------------------------------------------

    @Test
    void updateExpenseRejectsNonOwner() {
        ExpenseReport expense = expense(ExpenseStatus.PENDING, "jane.tech");
        when(expenseRepository.findById(1L)).thenReturn(Optional.of(expense));

        ExpenseReportRequest request = ExpenseReportRequest.builder()
                .title("x").amount(BigDecimal.ONE).category(ExpenseCategory.OTHER).build();

        assertThrows(AccessDeniedException.class, () -> service.updateExpense(1L, request, "someone.else"));
        verify(expenseRepository, never()).save(any());
    }

    @Test
    void updateExpenseRejectsNonPendingReport() {
        ExpenseReport expense = expense(ExpenseStatus.APPROVED, "jane.tech");
        when(expenseRepository.findById(1L)).thenReturn(Optional.of(expense));

        ExpenseReportRequest request = ExpenseReportRequest.builder()
                .title("x").amount(BigDecimal.ONE).category(ExpenseCategory.OTHER).build();

        assertThrows(IllegalStateException.class, () -> service.updateExpense(1L, request, "jane.tech"));
    }

    @Test
    void updateExpenseSucceedsForOwnerOnPendingReport() {
        ExpenseReport expense = expense(ExpenseStatus.PENDING, "jane.tech");
        when(expenseRepository.findById(1L)).thenReturn(Optional.of(expense));
        when(expenseRepository.save(any(ExpenseReport.class))).thenAnswer(inv -> inv.getArgument(0));

        ExpenseReportRequest request = ExpenseReportRequest.builder()
                .title("Updated title").amount(new BigDecimal("300.00")).category(ExpenseCategory.LABOR).build();

        ExpenseReportResponse response = service.updateExpense(1L, request, "jane.tech");

        assertEquals("Updated title", response.getTitle());
        assertEquals(new BigDecimal("300.00"), response.getAmount());
    }

    @Test
    void deleteExpenseRejectsNonOwner() {
        ExpenseReport expense = expense(ExpenseStatus.PENDING, "jane.tech");
        when(expenseRepository.findById(1L)).thenReturn(Optional.of(expense));

        assertThrows(AccessDeniedException.class, () -> service.deleteExpense(1L, "someone.else"));
        verify(expenseRepository, never()).delete(any());
    }

    // ------------------------------------------------------------------
    // getExpenseSummary: input validation + aggregation math
    // ------------------------------------------------------------------

    @Test
    void expenseSummaryRejectsInvalidMonth() {
        assertThrows(IllegalArgumentException.class, () -> service.getExpenseSummary(2026, 13));
    }

    @Test
    void expenseSummaryRejectsInvalidYear() {
        assertThrows(IllegalArgumentException.class, () -> service.getExpenseSummary(1999, 5));
    }

    @Test
    void expenseSummaryAggregatesByStatusAndCategory() {
        List<ExpenseReport> expenses = List.of(
                ExpenseReport.builder().status(ExpenseStatus.APPROVED).category(ExpenseCategory.PARTS).amount(new BigDecimal("100")).build(),
                ExpenseReport.builder().status(ExpenseStatus.APPROVED).category(ExpenseCategory.PARTS).amount(new BigDecimal("50")).build(),
                ExpenseReport.builder().status(ExpenseStatus.PENDING).category(ExpenseCategory.LABOR).amount(new BigDecimal("75")).build(),
                ExpenseReport.builder().status(ExpenseStatus.REJECTED).category(ExpenseCategory.OTHER).amount(new BigDecimal("20")).build()
        );
        when(expenseRepository.findByYearAndMonth(2026, 3)).thenReturn(expenses);

        ExpenseSummaryResponse summary = service.getExpenseSummary(2026, 3);

        assertEquals(4, summary.getTotalExpenseCount());
        assertEquals(2, summary.getApprovedCount());
        assertEquals(1, summary.getPendingCount());
        assertEquals(1, summary.getRejectedCount());
        assertEquals(new BigDecimal("150"), summary.getTotalApprovedAmount());
        assertEquals(new BigDecimal("75"), summary.getTotalPendingAmount());
        assertEquals(new BigDecimal("20"), summary.getTotalRejectedAmount());
        assertEquals(new BigDecimal("150"), summary.getAmountByCategory().get("PARTS"));
        assertEquals(new BigDecimal("75"), summary.getAmountByCategory().get("LABOR"));
        assertEquals("March 2026", summary.getMonthName());
    }
}
