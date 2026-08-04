package com.pfe.predictive.template.scheduler;

import com.pfe.predictive.template.service.RecurringMaintenanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically spawns due Maintenance work orders from active recurring
 * rules. Unlike the escalation/auto-reorder schedulers (shipped disabled —
 * incidental gap-fixes), this one ships enabled by default since Recurring
 * Maintenance is the feature itself, not a side-effect fix.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RecurringMaintenanceScheduler {

    private final RecurringMaintenanceService recurringMaintenanceService;

    @Value("${maintenance.recurrence-scheduler-enabled:true}")
    private boolean enabled;

    @Scheduled(fixedRateString = "${maintenance.recurrence-check-interval-ms:3600000}")
    public void generateDueMaintenance() {
        if (!enabled) return;
        try {
            int generated = recurringMaintenanceService.generateDueMaintenance();
            if (generated > 0) {
                log.info("Recurring maintenance scheduler generated {} new work order(s)", generated);
            }
        } catch (Exception ex) {
            log.error("Recurring maintenance scheduler failed: {}", ex.getMessage(), ex);
        }
    }
}
