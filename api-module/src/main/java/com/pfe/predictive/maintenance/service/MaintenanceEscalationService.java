package com.pfe.predictive.maintenance.service;

import com.pfe.predictive.audit.service.AuditEventService;
import com.pfe.predictive.common.service.EmailService;
import com.pfe.predictive.core.entity.Maintenance;
import com.pfe.predictive.core.entity.MaintenancePriority;
import com.pfe.predictive.core.entity.MaintenanceStatus;
import com.pfe.predictive.data.repository.MaintenanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Implements the escalation behavior that maintenance.days-overdue-threshold
 * and maintenance.auto-escalation-enabled previously configured but nothing
 * ever read (functional audit, business-logic item #10). Off by default —
 * flip maintenance.auto-escalation-enabled to true to activate.
 *
 * Escalation is monotonic and self-limiting rather than repeatedly bumping a
 * still-overdue record on every scheduler run: each pass raises priority by
 * exactly one level (LOW→MEDIUM→HIGH→CRITICAL) and stops once a record hits
 * CRITICAL, so a record left overdue for a long time climbs steadily instead
 * of oscillating or spamming notifications forever. There's no per-record
 * "already escalated this run" flag yet — priority itself is the state.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MaintenanceEscalationService {

    private static final List<MaintenanceStatus> ESCALATABLE_STATUSES =
            List.of(MaintenanceStatus.SCHEDULED, MaintenanceStatus.IN_PROGRESS);
    private static final List<String> ESCALATION_NOTIFY_ROLES = List.of("MANAGER", "ADMIN", "SUPER_ADMIN");

    private final MaintenanceRepository maintenanceRepository;
    private final EmailService emailService;
    private final AuditEventService auditEventService;

    @Value("${maintenance.auto-escalation-enabled:false}")
    private boolean autoEscalationEnabled;

    @Value("${maintenance.days-overdue-threshold:7}")
    private int daysOverdueThreshold;

    @Transactional
    public void escalateOverdueMaintenance() {
        if (!autoEscalationEnabled) {
            return;
        }

        LocalDateTime cutoff = LocalDateTime.now().minusDays(daysOverdueThreshold);
        List<Maintenance> overdue = maintenanceRepository.findByStatusInAndScheduledDateBefore(ESCALATABLE_STATUSES, cutoff);

        for (Maintenance maintenance : overdue) {
            MaintenancePriority next = nextPriority(maintenance.getPriority());
            if (next == maintenance.getPriority()) {
                continue; // already CRITICAL — nothing further to escalate to
            }

            MaintenancePriority previous = maintenance.getPriority();
            maintenance.setPriority(next);
            maintenanceRepository.save(maintenance);

            auditEventService.record("SYSTEM", "MAINTENANCE_AUTO_ESCALATED", "Maintenance", maintenance.getId(),
                    "priority " + previous + " -> " + next + ", overdue since " + maintenance.getScheduledDate());

            notifyEscalation(maintenance, previous, next);
        }

        if (!overdue.isEmpty()) {
            log.info("Maintenance escalation pass: {} overdue work order(s) reviewed", overdue.size());
        }
    }

    private MaintenancePriority nextPriority(MaintenancePriority current) {
        return switch (current) {
            case LOW -> MaintenancePriority.MEDIUM;
            case MEDIUM -> MaintenancePriority.HIGH;
            case HIGH -> MaintenancePriority.CRITICAL;
            case CRITICAL -> MaintenancePriority.CRITICAL;
        };
    }

    private void notifyEscalation(Maintenance maintenance, MaintenancePriority previous, MaintenancePriority next) {
        try {
            String subject = "Maintenance #" + maintenance.getId() + " auto-escalated to " + next;
            String body = "<div style=\"font-family:Arial,sans-serif;font-size:14px;color:#111827;\">"
                    + "<h2 style=\"margin:0 0 12px 0;\">Overdue Maintenance Escalated</h2>"
                    + "<p><strong>Work order:</strong> #" + maintenance.getId() + "</p>"
                    + "<p><strong>Machine ID:</strong> " + maintenance.getMachineId() + "</p>"
                    + "<p><strong>Was scheduled for:</strong> " + maintenance.getScheduledDate() + "</p>"
                    + "<p><strong>Priority:</strong> " + previous + " &rarr; " + next + "</p>"
                    + "<p style=\"margin-top:16px;color:#6b7280;font-size:12px;\">"
                    + "This work order has been open past the configured overdue threshold ("
                    + daysOverdueThreshold + " day(s)) and was escalated automatically.</p>"
                    + "</div>";
            emailService.sendEmailToUsersByRoles(ESCALATION_NOTIFY_ROLES, subject, body);
        } catch (Exception ex) {
            log.error("Failed to send escalation notification for maintenance {}: {}",
                    maintenance.getId(), ex.getMessage(), ex);
        }
    }
}
