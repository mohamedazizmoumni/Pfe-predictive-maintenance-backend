package com.pfe.predictive.finance.service;

import com.pfe.predictive.audit.service.AuditEventService;
import com.pfe.predictive.common.service.EmailService;
import com.pfe.predictive.common.service.MaintenanceReportEmailContent;
import com.pfe.predictive.core.entity.User;
import com.pfe.predictive.core.entity.finance.ChecklistItem;
import com.pfe.predictive.core.entity.finance.MaintenanceRapport;
import com.pfe.predictive.core.entity.finance.RapportPart;
import com.pfe.predictive.core.entity.finance.RapportStatus;
import com.pfe.predictive.core.entity.portal.CustomerMachineLink;
import com.pfe.predictive.data.repository.MachineRepository;
import com.pfe.predictive.data.repository.UserRepository;
import com.pfe.predictive.data.repository.finance.MaintenanceRapportRepository;
import com.pfe.predictive.data.repository.portal.CustomerMachineLinkRepository;
import com.pfe.predictive.finance.dto.ChecklistItemRequest;
import com.pfe.predictive.finance.dto.MaintenanceRapportRequest;
import com.pfe.predictive.finance.dto.MaintenanceRapportResponse;
import com.pfe.predictive.finance.dto.RapportApprovalRequest;
import com.pfe.predictive.finance.dto.RapportPartRequest;
import com.pfe.predictive.finance.mapper.MaintenanceRapportMapper;
import com.pfe.predictive.inventory.entity.InventoryUsage;
import com.pfe.predictive.inventory.entity.PartReservation;
import com.pfe.predictive.inventory.repository.InventoryUsageRepository;
import com.pfe.predictive.inventory.repository.PartRepository;
import com.pfe.predictive.inventory.service.PartReservationService;
import com.pfe.predictive.report.service.MaintenanceInterventionReportService;
import com.pfe.predictive.telemetry.replay.MachineTrajectoryManager;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MaintenanceRapportService {

    private final MaintenanceRapportRepository rapportRepository;
    private final MaintenanceRapportMapper rapportMapper;
    private final PartRepository partRepository;
    private final InventoryUsageRepository inventoryUsageRepository;
    private final MachineRepository machineRepository;
    private final MachineTrajectoryManager machineTrajectoryManager;
    private final AuditEventService auditEventService;
    private final AnnualBudgetService budgetService;
    private final PartReservationService partReservationService;
    private final MaintenanceInterventionReportService interventionReportService;
    private final EmailService emailService;
    private final UserRepository userRepository;
    private final CustomerMachineLinkRepository customerMachineLinkRepository;

    // No MaintenanceTemplate entity yet (see the enterprise blueprint, §09) so
    // there's no per-machine/per-category recurrence rule to consult here —
    // this is a simple fixed interval until that exists.
    @Value("${maintenance.default-recurrence-days:90}")
    private int defaultRecurrenceDays;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    private static final DateTimeFormatter REPORT_EMAIL_DATE_FORMAT = DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a");

    // These list endpoints have no client-driven paging yet - cap at a
    // generous size (most recent first) instead of loading every rapport
    // ever submitted.
    private static final int LIST_CAP = 200;
    private static final Sort MOST_RECENT_FIRST = Sort.by(Sort.Direction.DESC, "createdDate");

    // ─────────────────────────────────────────────────────────────────────────
    // CREATE
    // ─────────────────────────────────────────────────────────────────────────

    public MaintenanceRapportResponse createRapport(MaintenanceRapportRequest request,
                                                     String technicianUsername,
                                                     String technicianName) {
        log.info("Creating maintenance rapport '{}' by {}", request.getTitle(), technicianUsername);

        List<RapportPart> parts = request.getParts().stream()
                .map(this::toPart)
                .collect(Collectors.toList());

        List<ChecklistItem> checklistItems = request.getChecklistItems().stream()
                .map(this::toChecklistItem)
                .collect(Collectors.toList());

        BigDecimal partsCost = parts.stream()
                .map(RapportPart::getTotalCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal laborCost = request.getLaborCost();
        BigDecimal totalCost = laborCost.add(partsCost);

        MaintenanceRapport rapport = MaintenanceRapport.builder()
                .taskId(request.getTaskId())
                .machineId(request.getMachineId())
                .machineName(request.getMachineName())
                .technicianUsername(technicianUsername)
                .technicianName(technicianName)
                .title(request.getTitle())
                .description(request.getDescription())
                .workPerformed(request.getWorkPerformed())
                .partsReplaced(request.getPartsReplaced())
                .laborHours(request.getLaborHours())
                .laborCost(laborCost)
                .partsCost(partsCost)
                .totalCost(totalCost)
                .parts(parts)
                .checklistItems(checklistItems)
                .status(RapportStatus.PENDING_MANAGER_APPROVAL)
                .build();

        MaintenanceRapport saved = rapportRepository.save(rapport);
        log.info("Maintenance rapport created — id={}, totalCost={}", saved.getId(), saved.getTotalCost());

        recordPartUsage(saved, technicianUsername);

        return rapportMapper.toResponse(saved);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // READ
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<MaintenanceRapportResponse> getMyRapports(String username) {
        return rapportRepository.findByTechnicianUsername(username, PageRequest.of(0, LIST_CAP, MOST_RECENT_FIRST))
                .stream()
                .map(rapportMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MaintenanceRapportResponse> getAllRapports() {
        return rapportRepository.findAllByOrderByCreatedDateDesc(PageRequest.of(0, LIST_CAP))
                .stream()
                .map(rapportMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MaintenanceRapportResponse> getPendingManagerApprovals() {
        return rapportRepository
                .findByStatusOrderByCreatedDateDesc(RapportStatus.PENDING_MANAGER_APPROVAL, PageRequest.of(0, LIST_CAP))
                .stream()
                .map(rapportMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MaintenanceRapportResponse> getPendingFinanceApprovals() {
        return rapportRepository
                .findByStatusOrderByCreatedDateDesc(RapportStatus.PENDING_FINANCE_APPROVAL, PageRequest.of(0, LIST_CAP))
                .stream()
                .map(rapportMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MaintenanceRapportResponse getRapportById(Long id) {
        return rapportMapper.toResponse(findOrThrow(id));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // APPROVAL WORKFLOW
    // ─────────────────────────────────────────────────────────────────────────

    public MaintenanceRapportResponse reviewByManager(Long id, RapportApprovalRequest request, String reviewer) {
        log.info("Manager review of rapport id={} by {} — approved={}", id, reviewer, request.isApproved());

        MaintenanceRapport rapport = findOrThrow(id);
        requireStatus(rapport, RapportStatus.PENDING_MANAGER_APPROVAL);

        if (request.isApproved()) {
            requireFailedChecklistAcknowledgement(rapport, request);
        }

        rapport.setApprovedByManager(reviewer);
        rapport.setManagerApprovedDate(LocalDateTime.now());

        if (request.isApproved()) {
            rapport.setStatus(RapportStatus.PENDING_FINANCE_APPROVAL);
        } else {
            rapport.setStatus(RapportStatus.REJECTED);
            rapport.setRejectionReason(request.getRejectionReason());
            restorePartUsage(rapport, reviewer);
        }

        MaintenanceRapport saved = rapportRepository.save(rapport);
        auditEventService.record(reviewer,
                request.isApproved() ? "RAPPORT_MANAGER_APPROVED" : "RAPPORT_MANAGER_REJECTED",
                "MaintenanceRapport", saved.getId(),
                request.isApproved() && !saved.getChecklistItems().isEmpty()
                        && saved.getChecklistItems().stream().anyMatch(item -> !item.isPassed())
                        ? "machineId=" + saved.getMachineId() + "; approved despite failed checklist item(s): " + request.getReviewNote()
                        : "machineId=" + saved.getMachineId());

        if (request.isApproved()) {
            sendMaintenanceReportEmails(saved);
        }

        log.info("Rapport id={} manager review complete — status={}", id, saved.getStatus());
        return rapportMapper.toResponse(saved);
    }

    public MaintenanceRapportResponse reviewByFinance(Long id, RapportApprovalRequest request, String reviewer) {
        log.info("Finance review of rapport id={} by {} — approved={}", id, reviewer, request.isApproved());

        MaintenanceRapport rapport = findOrThrow(id);
        requireStatus(rapport, RapportStatus.PENDING_FINANCE_APPROVAL);

        rapport.setApprovedByFinance(reviewer);
        rapport.setFinanceApprovedDate(LocalDateTime.now());

        if (request.isApproved()) {
            rapport.setStatus(RapportStatus.APPROVED);
        } else {
            rapport.setStatus(RapportStatus.REJECTED);
            rapport.setRejectionReason(request.getRejectionReason());
            restorePartUsage(rapport, reviewer);
        }

        MaintenanceRapport saved = rapportRepository.save(rapport);
        auditEventService.record(reviewer,
                request.isApproved() ? "RAPPORT_FINANCE_APPROVED" : "RAPPORT_FINANCE_REJECTED",
                "MaintenanceRapport", saved.getId(),
                "machineId=" + saved.getMachineId());

        if (request.isApproved()) {
            applyMaintenanceCompletionEffects(saved);
            // The rapport's parts+labor cost is real spend that never had an
            // ExpenseReport of its own — without this, finance-approving a
            // rapport left the annual budget's spent/remaining figures
            // untouched, so the Finance Dashboard never reflected it.
            budgetService.recordApprovedExpense(saved.getTotalCost(), saved.getFinanceApprovedDate().getYear());
        }

        log.info("Rapport id={} finance review complete — status={}", id, saved.getStatus());
        return rapportMapper.toResponse(saved);
    }

    /**
     * A rapport reaching final (finance) approval means the repair it describes
     * is done and paid for — this is the platform's only signal that a machine
     * was actually fixed. Previously nothing happened here: machine health never
     * improved after maintenance and nextMaintenanceDate never advanced, so a
     * freshly-repaired machine could sit flagged as degraded until its next
     * telemetry tick happened to say otherwise (functional audit, business
     * logic item #8).
     *
     * Health/status specifically are restored via MachineTrajectoryManager,
     * not by writing Machine.status directly — the live replay scheduler
     * recomputes both from the trajectory every ~2s, so a one-off field write
     * here would just be overwritten by the next tick. Resetting the
     * trajectory index is what the simulation's own admin "reset" action does,
     * and is the only thing the live pipeline treats as authoritative.
     */
    private void applyMaintenanceCompletionEffects(MaintenanceRapport rapport) {
        if (rapport.getMachineId() == null) {
            return;
        }
        try {
            machineRepository.findById(rapport.getMachineId()).ifPresent(machine -> {
                LocalDateTime now = LocalDateTime.now();
                machine.setLastMaintenanceDate(now);
                machine.setNextMaintenanceDate(now.plusDays(defaultRecurrenceDays));
                machineRepository.save(machine);
            });
            machineTrajectoryManager.reset(rapport.getMachineId());
            log.info("Rapport id={} approved — machine {} health/trajectory reset, next maintenance in {}d",
                    rapport.getId(), rapport.getMachineId(), defaultRecurrenceDays);
        } catch (Exception ex) {
            // Never let a health-recalculation problem block a finance approval
            // that already succeeded and already restored/booked everything else.
            log.error("Failed to apply maintenance-completion effects for machine {} (rapport {}): {}",
                    rapport.getMachineId(), rapport.getId(), ex.getMessage(), ex);
        }
    }

    /**
     * Priority 6: once a manager approves a rapport, the Priority 5 PDF
     * intervention report is generated and emailed to that manager, and —
     * if the machine is linked to one or more customer accounts (Customer
     * Portal, see CustomerMachineLink) — each linked customer gets a
     * separate sanitized "maintenance completed" notice with no PDF and no
     * internal detail (cost, technician, work notes). Mirrors
     * applyMaintenanceCompletionEffects: never lets an email/report failure
     * block an approval that has already succeeded.
     */
    private void sendMaintenanceReportEmails(MaintenanceRapport rapport) {
        try {
            String machineLabel = rapport.getMachineName() != null && !rapport.getMachineName().isBlank()
                    ? rapport.getMachineName()
                    : "Machine #" + rapport.getMachineId();

            MaintenanceReportEmailContent content = new MaintenanceReportEmailContent(
                    rapport.getId(),
                    machineLabel,
                    rapport.getMachineId(),
                    rapport.getWorkPerformed(),
                    rapport.getTechnicianName() != null && !rapport.getTechnicianName().isBlank()
                            ? rapport.getTechnicianName() : rapport.getTechnicianUsername(),
                    rapport.getApprovedByManager(),
                    rapport.getManagerApprovedDate() != null ? rapport.getManagerApprovedDate().format(REPORT_EMAIL_DATE_FORMAT) : null,
                    rapport.getTotalCost() != null ? rapport.getTotalCost().toPlainString() + " TND" : null,
                    reportActionUrl()
            );

            if (rapport.getApprovedByManager() != null && !rapport.getApprovedByManager().isBlank()) {
                userRepository.findByUsername(rapport.getApprovedByManager()).ifPresentOrElse(manager -> {
                    if (manager.getEmail() != null && !manager.getEmail().isBlank()) {
                        byte[] pdf = interventionReportService.generate(rapport.getId());
                        emailService.sendMaintenanceReportEmail(manager.getEmail(), displayName(manager), content, pdf);
                    } else {
                        log.warn("Rapport id={} approved by '{}' but that user has no email on file — skipping manager report email",
                                rapport.getId(), rapport.getApprovedByManager());
                    }
                }, () -> log.warn("Rapport id={} approved but reviewer '{}' has no matching user account — skipping manager report email",
                        rapport.getId(), rapport.getApprovedByManager()));
            }

            if (rapport.getMachineId() != null) {
                for (CustomerMachineLink link : customerMachineLinkRepository.findByMachineId(rapport.getMachineId())) {
                    userRepository.findById(link.getUserId()).ifPresent(customer -> {
                        if (customer.getEmail() != null && !customer.getEmail().isBlank()) {
                            emailService.sendMaintenanceCompletedCustomerNotification(customer.getEmail(), displayName(customer), content);
                        }
                    });
                }
            }
        } catch (Exception ex) {
            // Same rationale as applyMaintenanceCompletionEffects: the approval
            // itself already succeeded and was already saved/audited above —
            // a report-generation or email problem must not roll that back.
            log.error("Failed to send maintenance report email(s) for rapport {}: {}", rapport.getId(), ex.getMessage(), ex);
        }
    }

    private String displayName(User user) {
        if (user.getDisplayName() != null && !user.getDisplayName().isBlank()) {
            return user.getDisplayName();
        }
        if (user.getFirstName() != null && !user.getFirstName().isBlank()) {
            return user.getFirstName();
        }
        return user.getUsername();
    }

    private String reportActionUrl() {
        String base = frontendUrl != null && !frontendUrl.isBlank() ? frontendUrl.trim() : "http://localhost:4200";
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/finance/rapports";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private ChecklistItem toChecklistItem(ChecklistItemRequest request) {
        return ChecklistItem.builder()
                .description(request.getDescription())
                .passed(request.isPassed())
                .notes(request.getNotes())
                .build();
    }

    private RapportPart toPart(RapportPartRequest request) {
        BigDecimal total = request.getUnitCost().multiply(BigDecimal.valueOf(request.getQuantity()));
        return RapportPart.builder()
                .partId(request.getPartId())
                .partName(request.getPartName())
                .partCode(request.getPartCode())
                .quantity(request.getQuantity())
                .unitCost(request.getUnitCost())
                .totalCost(total)
                .supplier(request.getSupplier())
                .build();
    }

    private MaintenanceRapport findOrThrow(Long id) {
        return rapportRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Maintenance rapport not found with id " + id));
    }

    /**
     * Implements enterprise-blueprint rule #29: a failed checklist item must
     * not pass through approval unnoticed. Doesn't hard-block approval
     * forever (a documented failure the technician will address in a follow-up
     * job is a legitimate real-world outcome) — instead it requires the
     * manager to type an explicit acknowledgement, which is captured in the
     * audit trail alongside the approval.
     */
    private void requireFailedChecklistAcknowledgement(MaintenanceRapport rapport, RapportApprovalRequest request) {
        boolean hasFailedItem = rapport.getChecklistItems() != null
                && rapport.getChecklistItems().stream().anyMatch(item -> !item.isPassed());
        if (hasFailedItem && (request.getReviewNote() == null || request.getReviewNote().isBlank())) {
            throw new IllegalStateException(
                    "Rapport id=" + rapport.getId() + " has one or more failed checklist items — "
                    + "provide reviewNote acknowledging them before approving.");
        }
    }

    private void requireStatus(MaintenanceRapport rapport, RapportStatus expected) {
        if (rapport.getStatus() != expected) {
            throw new IllegalStateException(
                    "Rapport id=" + rapport.getId() +
                    " cannot be reviewed at this step — current status is " + rapport.getStatus());
        }
    }

    /**
     * Decrements stock and logs real usage for each rapport part. Two paths,
     * tried in order per line:
     *
     * 1. Reservation-backed (part.getPartId() != null): consume the active
     *    PartReservation(s) held for this job (rapport.getTaskId() — despite
     *    the name, this is a Maintenance id in the technician-completion flow
     *    that produces these rapports, the same id PartReservation.maintenanceId
     *    uses) and this part, oldest first, up to the quantity actually used.
     *    PartReservationService.consume() does the real stock decrement, so
     *    nothing further happens here for the covered quantity — doing a
     *    second decrement would double-count it.
     * 2. Name-match fallback (no partId, or the reservation(s) didn't cover
     *    the full quantity — e.g. more was used than was reserved): decrement
     *    the catalog part matched by name for whatever quantity path 1 didn't
     *    already cover. This is the original, still-supported behavior for
     *    free-text part entries.
     *
     * restorePartUsage() below doesn't need to know which path a line took —
     * it just adds the same total quantity back to the one shared
     * Part.currentStock row, which is correct regardless of how it was drained.
     */
    private void recordPartUsage(MaintenanceRapport rapport, String technicianUsername) {
        for (RapportPart part : rapport.getParts()) {
            int remaining = part.getQuantity();

            if (part.getPartId() != null) {
                for (PartReservation reservation : partReservationService.findActiveReservations(rapport.getTaskId(), part.getPartId())) {
                    if (remaining <= 0) {
                        break;
                    }
                    int consumeNow = Math.min(remaining, reservation.getQuantityReserved());
                    partReservationService.consume(reservation.getId(), consumeNow);
                    remaining -= consumeNow;
                }
            }

            if (remaining <= 0) {
                continue;
            }

            final int uncoveredQuantity = remaining;
            partRepository.findByNameIgnoreCase(part.getPartName()).ifPresent(catalogPart -> {
                Integer currentStock = catalogPart.getCurrentStock();
                catalogPart.setCurrentStock(Math.max(0, (currentStock == null ? 0 : currentStock) - uncoveredQuantity));
                partRepository.save(catalogPart);

                InventoryUsage usage = InventoryUsage.builder()
                        .part(catalogPart)
                        .quantityUsed(uncoveredQuantity)
                        .taskId(rapport.getTaskId())
                        .reason("Maintenance rapport #" + rapport.getId())
                        .usedBy(technicianUsername)
                        .build();
                inventoryUsageRepository.save(usage);
            });
        }
    }

    /**
     * Reverses recordPartUsage()'s stock decrement when a rapport is rejected at
     * either approval stage. Without this, a rejected rapport still permanently
     * consumed the parts it listed — stock was deducted the moment the technician
     * submitted the rapport, before either human had reviewed it, and rejection
     * never gave that stock back. Safe to call at most once per rapport: the
     * PENDING_MANAGER_APPROVAL/PENDING_FINANCE_APPROVAL guard in requireStatus()
     * prevents reviewByManager/reviewByFinance from re-processing an
     * already-decided rapport, so this can't double-restore.
     */
    private void restorePartUsage(MaintenanceRapport rapport, String reviewer) {
        for (RapportPart part : rapport.getParts()) {
            partRepository.findByNameIgnoreCase(part.getPartName()).ifPresent(catalogPart -> {
                Integer currentStock = catalogPart.getCurrentStock();
                catalogPart.setCurrentStock((currentStock == null ? 0 : currentStock) + part.getQuantity());
                partRepository.save(catalogPart);

                InventoryUsage reversal = InventoryUsage.builder()
                        .part(catalogPart)
                        .quantityUsed(-part.getQuantity())
                        .taskId(rapport.getTaskId())
                        .reason("Rapport #" + rapport.getId() + " rejected — stock restored")
                        .usedBy(reviewer)
                        .build();
                inventoryUsageRepository.save(reversal);
            });
        }
        log.info("Rapport id={} rejected — restored stock for {} part line(s)", rapport.getId(), rapport.getParts().size());
    }
}
