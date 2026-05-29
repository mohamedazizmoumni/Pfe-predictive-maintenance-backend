package com.pfe.predictive.maintenancecost.service;

import com.pfe.predictive.maintenancecost.entity.FailureEvent;
import com.pfe.predictive.maintenancecost.entity.Machine;
import com.pfe.predictive.maintenancecost.event.FailureOccurredEvent;
import com.pfe.predictive.maintenancecost.repository.FailureEventRepository;
import com.pfe.predictive.maintenancecost.repository.MachineRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Failure Event Service
 * Records machine failures and publishes events
 * 
 * @author Finance Module
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class FailureEventService {

    private final FailureEventRepository failureRepository;
    private final MachineRepository machineRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Record machine failure
     * This will trigger automatic budget update via event
     * 
     * @param machineId Machine ID
     * @param failureType Type of failure
     * @param downtimeHours Actual downtime in hours
     * @return Created failure event
     */
    public FailureEvent recordFailure(Long machineId, String failureType, double downtimeHours) {
        log.warn("🚨 Recording failure - Machine: {}, Type: {}, Downtime: {}h", 
                 machineId, failureType, downtimeHours);

        Machine machine = machineRepository.findById(machineId)
            .orElseThrow(() -> new EntityNotFoundException("Machine not found: " + machineId));

        // Create failure event
        // totalCostIncurred will be auto-computed by @PrePersist
        FailureEvent failure = FailureEvent.builder()
            .machine(machine)
            .failureType(failureType)
            .actualDowntimeHours(downtimeHours)
            .occurredAt(LocalDateTime.now())
            .build();

        FailureEvent saved = failureRepository.save(failure);

        log.warn("💸 Failure recorded - ID: {}, Cost: {}", 
                 saved.getId(), saved.getTotalCostIncurred());

        // Publish event - this will trigger budget update
        log.info("📢 Publishing FailureOccurredEvent for failure: {}", saved.getId());
        eventPublisher.publishEvent(new FailureOccurredEvent(this, saved.getId()));

        return saved;
    }

    /**
     * Get failure event by ID
     * @param failureId Failure ID
     * @return Failure event
     */
    @Transactional(readOnly = true)
    public FailureEvent getFailureEvent(Long failureId) {
        return failureRepository.findById(failureId)
            .orElseThrow(() -> new EntityNotFoundException("Failure event not found: " + failureId));
    }
}
