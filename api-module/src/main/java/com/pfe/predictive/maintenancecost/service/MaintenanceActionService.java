package com.pfe.predictive.maintenancecost.service;

import com.pfe.predictive.maintenancecost.entity.MaintenanceAction;
import com.pfe.predictive.maintenancecost.enums.MaintenanceActionStatus;
import com.pfe.predictive.maintenancecost.event.MaintenanceCompletedEvent;
import com.pfe.predictive.maintenancecost.repository.MaintenanceActionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Maintenance Action Service
 * Manages maintenance actions and publishes events
 * 
 * @author Finance Module
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MaintenanceActionService {

    private final MaintenanceActionRepository actionRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Mark maintenance action as completed
     * This will trigger automatic budget update via event
     * 
     * @param actionId Action ID
     * @return Updated action
     */
    public MaintenanceAction completeMaintenanceAction(Long actionId) {
        log.info("✅ Completing maintenance action: {}", actionId);

        MaintenanceAction action = actionRepository.findById(actionId)
            .orElseThrow(() -> new EntityNotFoundException("Maintenance action not found: " + actionId));

        if (action.getStatus() == MaintenanceActionStatus.COMPLETED) {
            log.warn("Maintenance action {} is already completed", actionId);
            return action;
        }

        // Update status
        action.setStatus(MaintenanceActionStatus.COMPLETED);
        MaintenanceAction saved = actionRepository.save(action);

        // Publish event - this will trigger budget update
        log.info("📢 Publishing MaintenanceCompletedEvent for action: {}", actionId);
        eventPublisher.publishEvent(new MaintenanceCompletedEvent(this, actionId));

        return saved;
    }

    /**
     * Get maintenance action by ID
     * @param actionId Action ID
     * @return Maintenance action
     */
    @Transactional(readOnly = true)
    public MaintenanceAction getMaintenanceAction(Long actionId) {
        return actionRepository.findById(actionId)
            .orElseThrow(() -> new EntityNotFoundException("Maintenance action not found: " + actionId));
    }
}
