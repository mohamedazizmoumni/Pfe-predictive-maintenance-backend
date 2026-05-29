package com.pfe.predictive.maintenancecost.event;

import com.pfe.predictive.maintenancecost.service.MaintenanceCostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Financial Event Listener
 * Automatically updates budgets when maintenance or failures occur
 * 
 * THIS IS THE AUTOMATION ENGINE 🤖
 * 
 * @author Finance Module
 * @version 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FinancialEventListener {

    private final MaintenanceCostService costService;

    /**
     * Handle maintenance completion
     * Automatically updates budget with maintenance costs
     */
    @Async
    @EventListener
    @Transactional
    public void handleMaintenanceCompleted(MaintenanceCompletedEvent event) {
        log.info("🎯 Event received: Maintenance completed - ID: {}", event.getMaintenanceActionId());
        
        try {
            costService.handleMaintenanceCompleted(event.getMaintenanceActionId());
            log.info("✅ Budget updated successfully for maintenance: {}", event.getMaintenanceActionId());
        } catch (Exception e) {
            log.error("❌ Failed to update budget for maintenance {}: {}", 
                     event.getMaintenanceActionId(), e.getMessage(), e);
        }
    }

    /**
     * Handle failure occurrence
     * Automatically updates budget with failure costs
     */
    @Async
    @EventListener
    @Transactional
    public void handleFailureOccurred(FailureOccurredEvent event) {
        log.info("🎯 Event received: Failure occurred - ID: {}", event.getFailureEventId());
        
        try {
            costService.handleFailureEvent(event.getFailureEventId());
            log.info("✅ Budget updated successfully for failure: {}", event.getFailureEventId());
        } catch (Exception e) {
            log.error("❌ Failed to update budget for failure {}: {}", 
                     event.getFailureEventId(), e.getMessage(), e);
        }
    }
}
