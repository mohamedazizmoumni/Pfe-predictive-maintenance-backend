package com.pfe.predictive.fleet.scheduler;

import com.pfe.predictive.fleet.service.FleetHealthDigestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically sends the fleet health digest email (Priority 7). Enabled by
 * default — like RecurringMaintenanceScheduler, this is the feature itself
 * rather than a side-effect fix, so it doesn't ship opted-out.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FleetHealthDigestScheduler {

    private final FleetHealthDigestService digestService;

    @Value("${fleet.digest.enabled:true}")
    private boolean enabled;

    @Scheduled(fixedRateString = "${fleet.digest.interval-ms:604800000}")
    public void sendWeeklyDigest() {
        if (!enabled) {
            return;
        }
        try {
            digestService.sendDigest();
        } catch (Exception ex) {
            log.error("Fleet health digest failed: {}", ex.getMessage(), ex);
        }
    }
}
