package com.pfe.predictive.maintenance.scheduler;

import com.pfe.predictive.maintenance.service.MaintenanceEscalationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically checks for overdue maintenance work orders and escalates them.
 * No-ops immediately (see MaintenanceEscalationService) when
 * maintenance.auto-escalation-enabled is false, which is the shipped default.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MaintenanceEscalationScheduler {

    private final MaintenanceEscalationService escalationService;

    @Scheduled(fixedRateString = "${maintenance.escalation-check-interval-ms:3600000}")
    public void checkOverdueMaintenance() {
        try {
            escalationService.escalateOverdueMaintenance();
        } catch (Exception ex) {
            log.error("Maintenance escalation check failed: {}", ex.getMessage(), ex);
        }
    }
}
