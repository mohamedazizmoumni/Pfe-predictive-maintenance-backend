package com.pfe.predictive.executive.service;

import com.pfe.predictive.core.entity.Machine;
import com.pfe.predictive.core.entity.MaintenanceStatus;
import com.pfe.predictive.core.entity.MaintenanceType;
import com.pfe.predictive.core.entity.finance.ExpenseStatus;
import com.pfe.predictive.core.entity.finance.RapportStatus;
import com.pfe.predictive.core.entity.portal.SupportTicketStatus;
import com.pfe.predictive.data.repository.MachineRepository;
import com.pfe.predictive.data.repository.MaintenanceRepository;
import com.pfe.predictive.data.repository.finance.ExpenseReportRepository;
import com.pfe.predictive.data.repository.finance.MaintenanceRapportRepository;
import com.pfe.predictive.data.repository.portal.SupportTicketRepository;
import com.pfe.predictive.executive.dto.ExecutiveSummaryDto;
import com.pfe.predictive.finance.dto.BudgetResponse;
import com.pfe.predictive.finance.service.AnnualBudgetService;
import com.pfe.predictive.inventory.entity.ReorderStatus;
import com.pfe.predictive.inventory.repository.ReorderRequestRepository;
import com.pfe.predictive.reliability.service.ReliabilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExecutiveSummaryService {

    private static final List<MaintenanceStatus> OPEN_STATUSES = List.of(MaintenanceStatus.SCHEDULED, MaintenanceStatus.IN_PROGRESS);
    private static final List<SupportTicketStatus> OPEN_TICKET_STATUSES = List.of(SupportTicketStatus.OPEN, SupportTicketStatus.IN_PROGRESS);
    private static final int MAINTENANCE_TYPE_WINDOW_DAYS = 30;

    private final MachineRepository machineRepository;
    private final MaintenanceRepository maintenanceRepository;
    private final MaintenanceRapportRepository maintenanceRapportRepository;
    private final ExpenseReportRepository expenseReportRepository;
    private final ReorderRequestRepository reorderRequestRepository;
    private final SupportTicketRepository supportTicketRepository;
    private final AnnualBudgetService annualBudgetService;
    private final ReliabilityService reliabilityService;

    public ExecutiveSummaryDto getSummary() {
        List<Machine> machines = machineRepository.findAll();

        java.util.OptionalDouble avgHealth = machines.stream()
                .map(Machine::getRiskScore)
                .filter(java.util.Objects::nonNull)
                .mapToDouble(risk -> Math.max(0.0, Math.min(100.0, 100.0 - risk)))
                .average();

        long openWorkOrders = maintenanceRepository.countByStatusIn(OPEN_STATUSES);
        long overdueWorkOrders = maintenanceRepository.findByStatusInAndScheduledDateBefore(OPEN_STATUSES, LocalDateTime.now()).size();

        long pendingRapports = maintenanceRapportRepository.countByStatus(RapportStatus.PENDING_MANAGER_APPROVAL)
                + maintenanceRapportRepository.countByStatus(RapportStatus.PENDING_FINANCE_APPROVAL);
        long pendingExpenses = expenseReportRepository.countByStatus(ExpenseStatus.PENDING);
        long pendingReorders = reorderRequestRepository.countByStatus(ReorderStatus.REQUESTED);
        long openTickets = supportTicketRepository.countByStatusIn(OPEN_TICKET_STATUSES);

        java.math.BigDecimal budgetUtilization = annualBudgetService.getCurrentYearBudget()
                .map(BudgetResponse::getUtilizationPercentage)
                .orElse(null);

        LocalDateTime maintenanceTypeWindowStart = LocalDateTime.now().minusDays(MAINTENANCE_TYPE_WINDOW_DAYS);
        long preventiveCompleted = maintenanceRepository.countByTypeAndCompletedDateAfter(MaintenanceType.PREVENTIVE, maintenanceTypeWindowStart);
        long correctiveCompleted = maintenanceRepository.countByTypeAndCompletedDateAfter(MaintenanceType.CORRECTIVE, maintenanceTypeWindowStart)
                + maintenanceRepository.countByTypeAndCompletedDateAfter(MaintenanceType.EMERGENCY, maintenanceTypeWindowStart);

        return ExecutiveSummaryDto.builder()
                .machineCount(machines.size())
                .fleetAverageHealth(avgHealth.isPresent() ? avgHealth.getAsDouble() : null)
                .openWorkOrders(openWorkOrders)
                .overdueWorkOrders(overdueWorkOrders)
                .pendingRapportApprovals(pendingRapports)
                .pendingExpenseApprovals(pendingExpenses)
                .pendingReorderApprovals(pendingReorders)
                .openSupportTickets(openTickets)
                .budgetUtilizationPercentage(budgetUtilization)
                .topReliabilityRisks(reliabilityService.getFleetReliability().stream().limit(3).toList())
                .preventiveCompletedLast30Days(preventiveCompleted)
                .correctiveCompletedLast30Days(correctiveCompleted)
                .build();
    }
}
