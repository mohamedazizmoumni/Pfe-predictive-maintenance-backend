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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ExpenseReportService {

    private final ExpenseReportRepository expenseRepository;
    private final ExpenseReportMapper expenseMapper;
    private final AnnualBudgetService budgetService;
    private final AuditEventService auditEventService;

    // These list endpoints have no client-driven paging yet - cap at a
    // generous size (most recent first) instead of loading every row ever
    // submitted.
    private static final int LIST_CAP = 200;
    private static final Sort MOST_RECENT_FIRST = Sort.by(Sort.Direction.DESC, "createdDate");

    // ─────────────────────────────────────────────────────────────────────────
    // CREATE
    // ─────────────────────────────────────────────────────────────────────────

    public ExpenseReportResponse createExpense(ExpenseReportRequest request,
                                               String submittedBy,
                                               String submittedByName) {
        log.info("Creating expense report '{}' by {}", request.getTitle(), submittedBy);

        ExpenseReport expense = ExpenseReport.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .amount(request.getAmount())
                .category(request.getCategory())
                .machineId(request.getMachineId())
                .machineName(request.getMachineName())
                .maintenanceTaskId(request.getMaintenanceTaskId())
                .submittedBy(submittedBy)
                .submittedByName(submittedByName)
                .reviewNote(request.getNotes())
                .status(ExpenseStatus.PENDING)
                .build();

        ExpenseReport saved = expenseRepository.save(expense);
        log.info("Expense report created — id={}, amount={}", saved.getId(), saved.getAmount());
        return expenseMapper.toResponse(saved);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // READ
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ExpenseReportResponse> getMyExpenses(String username) {
        log.info("Fetching expenses for user {}", username);
        return expenseRepository.findBySubmittedBy(username, PageRequest.of(0, LIST_CAP, MOST_RECENT_FIRST))
                .stream()
                .map(expenseMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ExpenseReportResponse> getAllExpenses() {
        log.info("Fetching all expense reports");
        return expenseRepository.findAllByOrderByCreatedDateDesc(PageRequest.of(0, LIST_CAP))
                .stream()
                .map(expenseMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ExpenseReportResponse> getPendingExpenses() {
        log.info("Fetching pending expense reports");
        return expenseRepository
                .findByStatusOrderByCreatedDateDesc(ExpenseStatus.PENDING, PageRequest.of(0, LIST_CAP))
                .stream()
                .map(expenseMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ExpenseReportResponse getExpenseById(Long id) {
        log.info("Fetching expense report id={}", id);
        return expenseRepository.findById(id)
                .map(expenseMapper::toResponse)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Expense report not found with id " + id));
    }

    @Transactional(readOnly = true)
    public List<ExpenseReportResponse> getExpensesByCategory(ExpenseCategory category) {
        log.info("Fetching expense reports by category {}", category);
        return expenseRepository.findByCategory(category, PageRequest.of(0, LIST_CAP, MOST_RECENT_FIRST))
                .stream()
                .map(expenseMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ExpenseReportResponse> getExpensesByMachine(Long machineId) {
        log.info("Fetching expense reports for machine id={}", machineId);
        return expenseRepository.findByMachineId(machineId, PageRequest.of(0, LIST_CAP, MOST_RECENT_FIRST))
                .stream()
                .map(expenseMapper::toResponse)
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // APPROVE / REJECT
    // ─────────────────────────────────────────────────────────────────────────

    public ExpenseReportResponse approveExpense(Long id,
                                                ApproveExpenseRequest request,
                                                String reviewedBy) {
        log.info("Approving expense report id={} by {}", id, reviewedBy);

        ExpenseReport expense = findOrThrow(id);
        requirePending(expense);

        expense.setStatus(ExpenseStatus.APPROVED);
        expense.setReviewedBy(reviewedBy);
        expense.setReviewedDate(LocalDateTime.now());
        expense.setReviewNote(request.getReviewNote());

        ExpenseReport saved = expenseRepository.save(expense);

        // Update the annual budget
        int year = saved.getCreatedDate().getYear();
        budgetService.recordApprovedExpense(saved.getAmount(), year);

        auditEventService.record(reviewedBy, "EXPENSE_APPROVED", "ExpenseReport", saved.getId(),
                "amount=" + saved.getAmount());

        log.info("Expense report id={} approved — amount={}", id, saved.getAmount());
        return expenseMapper.toResponse(saved);
    }

    public ExpenseReportResponse rejectExpense(Long id,
                                               RejectExpenseRequest request,
                                               String reviewedBy) {
        log.info("Rejecting expense report id={} by {}", id, reviewedBy);

        ExpenseReport expense = findOrThrow(id);
        requirePending(expense);

        expense.setStatus(ExpenseStatus.REJECTED);
        expense.setReviewedBy(reviewedBy);
        expense.setReviewedDate(LocalDateTime.now());
        expense.setRejectionReason(request.getRejectionReason());

        ExpenseReport saved = expenseRepository.save(expense);
        auditEventService.record(reviewedBy, "EXPENSE_REJECTED", "ExpenseReport", saved.getId(),
                "amount=" + saved.getAmount());
        log.info("Expense report id={} rejected", id);
        return expenseMapper.toResponse(saved);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UPDATE / DELETE
    // ─────────────────────────────────────────────────────────────────────────

    public ExpenseReportResponse updateExpense(Long id,
                                               ExpenseReportRequest request,
                                               String username) {
        log.info("Updating expense report id={} by {}", id, username);

        ExpenseReport expense = findOrThrow(id);
        requireOwner(expense, username);
        requirePending(expense);

        expense.setTitle(request.getTitle());
        expense.setDescription(request.getDescription());
        expense.setAmount(request.getAmount());
        expense.setCategory(request.getCategory());
        expense.setMachineId(request.getMachineId());
        expense.setMachineName(request.getMachineName());
        expense.setMaintenanceTaskId(request.getMaintenanceTaskId());
        if (request.getNotes() != null) {
            expense.setReviewNote(request.getNotes());
        }

        ExpenseReport saved = expenseRepository.save(expense);
        log.info("Expense report id={} updated", id);
        return expenseMapper.toResponse(saved);
    }

    public void deleteExpense(Long id, String username) {
        log.info("Deleting expense report id={} by {}", id, username);

        ExpenseReport expense = findOrThrow(id);
        requireOwner(expense, username);
        requirePending(expense);

        expenseRepository.delete(expense);
        log.info("Expense report id={} deleted", id);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SUMMARY
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ExpenseSummaryResponse getExpenseSummary(int year, int month) {
        log.info("Fetching expense summary for {}/{}", month, year);

        // Validate inputs
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("Month must be between 1 and 12");
        }
        if (year < 2000 || year > 2099) {
            throw new IllegalArgumentException("Year must be between 2000 and 2099");
        }

        // Fetch expenses for the given month and year
        List<ExpenseReport> expenses = expenseRepository.findByYearAndMonth(year, month);

        // Compute counts and sums by status
        long approvedCount = expenses.stream()
                .filter(e -> e.getStatus() == ExpenseStatus.APPROVED)
                .count();
        long pendingCount = expenses.stream()
                .filter(e -> e.getStatus() == ExpenseStatus.PENDING)
                .count();
        long rejectedCount = expenses.stream()
                .filter(e -> e.getStatus() == ExpenseStatus.REJECTED)
                .count();
        long totalExpenseCount = expenses.size();

        BigDecimal totalApprovedAmount = expenses.stream()
                .filter(e -> e.getStatus() == ExpenseStatus.APPROVED)
                .map(ExpenseReport::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPendingAmount = expenses.stream()
                .filter(e -> e.getStatus() == ExpenseStatus.PENDING)
                .map(ExpenseReport::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalRejectedAmount = expenses.stream()
                .filter(e -> e.getStatus() == ExpenseStatus.REJECTED)
                .map(ExpenseReport::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Compute amounts by category (all 5 categories always present)
        Map<String, BigDecimal> amountByCategory = new HashMap<>();
        for (ExpenseCategory category : ExpenseCategory.values()) {
            BigDecimal amount = expenses.stream()
                    .filter(e -> e.getCategory() == category)
                    .map(ExpenseReport::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            amountByCategory.put(category.name(), amount);
        }

        // Format month name
        String monthName = Month.of(month).getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + year;

        log.info("Expense summary for {}/{} — total={}, approved={}, pending={}, rejected={}",
                month, year, totalExpenseCount, approvedCount, pendingCount, rejectedCount);

        return ExpenseSummaryResponse.builder()
                .year(year)
                .month(month)
                .monthName(monthName)
                .totalApprovedAmount(totalApprovedAmount)
                .totalPendingAmount(totalPendingAmount)
                .totalRejectedAmount(totalRejectedAmount)
                .approvedCount(approvedCount)
                .pendingCount(pendingCount)
                .rejectedCount(rejectedCount)
                .totalExpenseCount(totalExpenseCount)
                .amountByCategory(amountByCategory)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private ExpenseReport findOrThrow(Long id) {
        return expenseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Expense report not found with id " + id));
    }

    private void requirePending(ExpenseReport expense) {
        if (expense.getStatus() != ExpenseStatus.PENDING) {
            throw new IllegalStateException(
                    "Expense report id=" + expense.getId() +
                    " cannot be modified — current status is " + expense.getStatus());
        }
    }

    private void requireOwner(ExpenseReport expense, String username) {
        if (!expense.getSubmittedBy().equals(username)) {
            throw new AccessDeniedException(
                    "You are not allowed to modify expense report id=" + expense.getId());
        }
    }
}
