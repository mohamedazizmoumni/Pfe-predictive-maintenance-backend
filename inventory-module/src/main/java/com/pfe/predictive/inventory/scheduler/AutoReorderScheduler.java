package com.pfe.predictive.inventory.scheduler;

import com.pfe.predictive.inventory.service.AutoReorderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically checks for parts below the auto-reorder trigger level and
 * drafts reorder requests. No-ops immediately (see AutoReorderService) when
 * inventory.auto-reorder-enabled is false, which is the shipped default.
 * Scheduling itself is enabled application-wide by @EnableScheduling on
 * PredictiveMaintenanceApplication (api-module) — this component only needs
 * to be on the classpath and component-scanned, which it is since
 * inventory-module is a dependency of api-module.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AutoReorderScheduler {

    private final AutoReorderService autoReorderService;

    @Scheduled(fixedRateString = "${inventory.auto-reorder-check-interval-ms:3600000}")
    public void checkLowStock() {
        try {
            autoReorderService.draftReordersForPartsBelowTrigger();
        } catch (Exception ex) {
            log.error("Auto-reorder check failed: {}", ex.getMessage(), ex);
        }
    }
}
