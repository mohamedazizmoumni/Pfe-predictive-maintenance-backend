package com.pfe.predictive.maintenance.service;

import com.pfe.predictive.maintenance.dto.MaintenanceRequest;
import com.pfe.predictive.core.entity.Maintenance;
import com.pfe.predictive.core.entity.MaintenanceStatus;
import com.pfe.predictive.core.entity.MaintenanceType;
import com.pfe.predictive.data.repository.MaintenanceRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Maintenance Service
 * Handles write operations for maintenance scheduling and execution
 * Manages maintenance lifecycle: SCHEDULED → IN_PROGRESS → COMPLETED → APPROVED
 *
 * @author Maintenance Module
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MaintenanceService {

    private final MaintenanceRepository maintenanceRepository;

    /**
     * Create new maintenance task
     * @param request Maintenance creation request
     * @param createdBy User creating task
     * @return Created maintenance task
     */
    public Maintenance createMaintenance(MaintenanceRequest request, String createdBy) {
        log.info("Creating maintenance task for machine: {}, Type: {}", request.getMachineId(), request.getType());

        Maintenance maintenance = Maintenance.builder()
            .machineId(request.getMachineId())
            .type(MaintenanceType.valueOf(request.getType()))
            .description(request.getDescription())
            .scheduledDate(request.getScheduledDate())
            .estimatedDuration(request.getEstimatedDuration())
            .status(MaintenanceStatus.SCHEDULED)
            .assignedTechnicianId(request.getAssignedTechnicianId())
            .priority(request.getPriority())
            .createdBy(createdBy)
            .build();

        Maintenance saved = maintenanceRepository.save(maintenance);
        log.debug("Maintenance task created with ID: {}", saved.getId());
        return saved;
    }

    /**
     * Update maintenance task details
     * @param maintenanceId Task ID to update
     * @param request Update request
     * @return Updated task
     */
    public Maintenance updateMaintenance(Long maintenanceId, MaintenanceRequest request) {
        log.info("Updating maintenance task: {}", maintenanceId);

        Maintenance maintenance = maintenanceRepository.findById(maintenanceId)
            .orElseThrow(() -> new EntityNotFoundException("Maintenance task not found: " + maintenanceId));

        if (request.getDescription() != null) {
            maintenance.setDescription(request.getDescription());
        }
        if (request.getScheduledDate() != null && maintenance.getStatus() == MaintenanceStatus.SCHEDULED) {
            maintenance.setScheduledDate(request.getScheduledDate());
        }
        if (request.getAssignedTechnicianId() != null) {
            maintenance.setAssignedTechnicianId(request.getAssignedTechnicianId());
        }
        if (request.getPriority() != null) {
            maintenance.setPriority(request.getPriority());
        }

        Maintenance updated = maintenanceRepository.save(maintenance);
        log.debug("Maintenance task updated: {}", maintenanceId);
        return updated;
    }

    /**
     * Start maintenance work
     * @param maintenanceId Task ID
     * @return Updated maintenance task
     */
    public Maintenance startMaintenance(Long maintenanceId) {
        log.info("Starting maintenance task: {}", maintenanceId);

        Maintenance maintenance = maintenanceRepository.findById(maintenanceId)
            .orElseThrow(() -> new EntityNotFoundException("Maintenance task not found: " + maintenanceId));

        if (maintenance.getStatus() != MaintenanceStatus.SCHEDULED) {
            throw new IllegalStateException("Can only start SCHEDULED maintenance tasks");
        }

        maintenance.setStatus(MaintenanceStatus.IN_PROGRESS);
        maintenance.setStartDate(LocalDateTime.now());

        return maintenanceRepository.save(maintenance);
    }

    /**
     * Complete maintenance work
     * @param maintenanceId Task ID
     * @param notes Completion notes
     * @return Updated task
     */
    public Maintenance completeMaintenance(Long maintenanceId, String notes) {
        log.info("Completing maintenance task: {}", maintenanceId);

        Maintenance maintenance = maintenanceRepository.findById(maintenanceId)
            .orElseThrow(() -> new EntityNotFoundException("Maintenance task not found: " + maintenanceId));

        if (maintenance.getStatus() != MaintenanceStatus.IN_PROGRESS) {
            throw new IllegalStateException("Can only complete IN_PROGRESS maintenance tasks");
        }

        maintenance.setStatus(MaintenanceStatus.COMPLETED);
        maintenance.setCompletedDate(LocalDateTime.now());
        maintenance.setNotes(notes);

        log.debug("Maintenance task completed: {}", maintenanceId);
        return maintenanceRepository.save(maintenance);
    }

    /**
     * Approve completed maintenance
     * @param maintenanceId Task ID
     * @param approvedBy User approving
     * @return Updated task
     */
    public Maintenance approveMaintenance(Long maintenanceId, String approvedBy) {
        log.info("Approving maintenance task: {}", maintenanceId);

        Maintenance maintenance = maintenanceRepository.findById(maintenanceId)
            .orElseThrow(() -> new EntityNotFoundException("Maintenance task not found: " + maintenanceId));

        if (maintenance.getStatus() != MaintenanceStatus.COMPLETED) {
            throw new IllegalStateException("Can only approve COMPLETED maintenance tasks");
        }

        maintenance.setStatus(MaintenanceStatus.APPROVED);
        maintenance.setApprovedDate(LocalDateTime.now());
        maintenance.setApprovedBy(approvedBy);

        log.debug("Maintenance task approved: {}", maintenanceId);
        return maintenanceRepository.save(maintenance);
    }

    /**
     * Reject completed maintenance (send back to IN_PROGRESS)
     * @param maintenanceId Task ID
     * @param reason Rejection reason
     * @return Updated task
     */
    public Maintenance rejectMaintenance(Long maintenanceId, String reason) {
        log.info("Rejecting maintenance task: {}", maintenanceId);

        Maintenance maintenance = maintenanceRepository.findById(maintenanceId)
            .orElseThrow(() -> new EntityNotFoundException("Maintenance task not found: " + maintenanceId));

        if (maintenance.getStatus() != MaintenanceStatus.COMPLETED) {
            throw new IllegalStateException("Can only reject COMPLETED maintenance tasks");
        }

        maintenance.setStatus(MaintenanceStatus.IN_PROGRESS);
        maintenance.setCompletedDate(null);
        maintenance.setNotes(reason);

        log.warn("Maintenance task rejected: {}, Reason: {}", maintenanceId, reason);
        return maintenanceRepository.save(maintenance);
    }

    /**
     * Cancel maintenance task
     * @param maintenanceId Task ID
     * @param reason Cancellation reason
     * @return Updated task
     */
    public Maintenance cancelMaintenance(Long maintenanceId, String reason) {
        log.info("Cancelling maintenance task: {}", maintenanceId);

        Maintenance maintenance = maintenanceRepository.findById(maintenanceId)
            .orElseThrow(() -> new EntityNotFoundException("Maintenance task not found: " + maintenanceId));

        if (maintenance.getStatus() == MaintenanceStatus.APPROVED || maintenance.getStatus() == MaintenanceStatus.CANCELLED) {
            throw new IllegalStateException("Cannot cancel " + maintenance.getStatus() + " tasks");
        }

        maintenance.setStatus(MaintenanceStatus.CANCELLED);
        maintenance.setNotes("CANCELLED: " + reason);

        return maintenanceRepository.save(maintenance);
    }

    /**
     * Delete maintenance task
     * @param maintenanceId Task ID
     */
    public void deleteMaintenance(Long maintenanceId) {
        log.info("Deleting maintenance task: {}", maintenanceId);

        Maintenance maintenance = maintenanceRepository.findById(maintenanceId)
            .orElseThrow(() -> new EntityNotFoundException("Maintenance task not found: " + maintenanceId));

        maintenanceRepository.delete(maintenance);
        log.debug("Maintenance task deleted: {}", maintenanceId);
    }
}
