package com.pfe.predictive.maintenance.service;

import com.pfe.predictive.core.entity.Maintenance;
import com.pfe.predictive.core.entity.MaintenanceStatus;
import com.pfe.predictive.core.entity.MaintenanceType;
import com.pfe.predictive.data.repository.MaintenanceRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Maintenance Query Service
 * Handles read-only operations for maintenance tasks and analytics
 * All operations use read-only transactions for optimal performance
 *
 * @author Maintenance Module
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class MaintenanceQueryService {

    private final MaintenanceRepository maintenanceRepository;

    /**
     * Get maintenance task by ID
     * @param maintenanceId Task ID
     * @return Maintenance task
     */
    public Maintenance getMaintenanceById(Long maintenanceId) {
        log.debug("Fetching maintenance task: {}", maintenanceId);
        return maintenanceRepository.findById(maintenanceId)
            .orElseThrow(() -> new EntityNotFoundException("Maintenance task not found: " + maintenanceId));
    }

    /**
     * Get all maintenance tasks paginated
     * @param pageable Pagination info
     * @return Page of tasks
     */
    public Page<Maintenance> getAllMaintenance(Pageable pageable) {
        log.debug("Fetching all maintenance tasks, page: {}, size: {}", pageable.getPageNumber(), pageable.getPageSize());
        return maintenanceRepository.findAll(pageable);
    }

    /**
     * Get maintenance tasks by status
     * @param status Task status
     * @param pageable Pagination info
     * @return Page of tasks
     */
    public Page<Maintenance> getMaintenanceByStatus(MaintenanceStatus status, Pageable pageable) {
        log.debug("Fetching maintenance tasks by status: {}", status);
        return maintenanceRepository.findByStatus(status, pageable);
    }

    /**
     * Get maintenance tasks by type
     * @param type Maintenance type
     * @param pageable Pagination info
     * @return Page of tasks
     */
    public Page<Maintenance> getMaintenanceByType(MaintenanceType type, Pageable pageable) {
        log.debug("Fetching maintenance tasks by type: {}", type);
        return maintenanceRepository.findByType(type, pageable);
    }

    /**
     * Get maintenance tasks for specific machine
     * @param machineId Machine ID
     * @param pageable Pagination info
     * @return Page of tasks
     */
    public Page<Maintenance> getMaintenanceForMachine(Long machineId, Pageable pageable) {
        log.debug("Fetching maintenance tasks for machine: {}", machineId);
        return maintenanceRepository.findByMachineId(machineId, pageable);
    }

    /**
     * Get assigned maintenance tasks for technician
     * @param technicianId Technician ID
     * @param pageable Pagination info
     * @return Page of tasks
     */
    public Page<Maintenance> getAssignedMaintenance(Long technicianId, Pageable pageable) {
        log.debug("Fetching maintenance tasks assigned to technician: {}", technicianId);
        return maintenanceRepository.findByAssignedTechnicianId(technicianId, pageable);
    }

    /**
     * Get all pending maintenance (SCHEDULED or IN_PROGRESS)
     * @param pageable Pagination info
     * @return Page of tasks
     */
    public Page<Maintenance> getPendingMaintenance(Pageable pageable) {
        log.debug("Fetching pending maintenance tasks");
        return maintenanceRepository.findPendingMaintenance(pageable);
    }

    /**
     * Get overdue maintenance (scheduled date in past)
     * @param pageable Pagination info
     * @return Page of tasks
     */
    public Page<Maintenance> getOverdueMaintenance(Pageable pageable) {
        log.debug("Fetching overdue maintenance tasks");
        return maintenanceRepository.findOverdueMaintenance(pageable);
    }

    /**
     * Get completed maintenance for machine
     * @param machineId Machine ID
     * @param pageable Pagination info
     * @return Page of tasks
     */
    public Page<Maintenance> getCompletedMaintenanceForMachine(Long machineId, Pageable pageable) {
        log.debug("Fetching completed maintenance for machine: {}", machineId);
        return maintenanceRepository.findCompletedMaintenanceForMachine(machineId, pageable);
    }

    /**
     * Get maintenance tasks pending approval
     * @param pageable Pagination info
     * @return Page of tasks
     */
    public Page<Maintenance> getPendingApproval(Pageable pageable) {
        log.debug("Fetching maintenance tasks pending approval");
        return maintenanceRepository.findPendingApproval(pageable);
    }

    /**
     * Get maintenance tasks scheduled for date range
     * @param startDate Start date
     * @param endDate End date
     * @return List of tasks
     */
    public List<Maintenance> getMaintenanceByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        log.debug("Fetching maintenance tasks between {} and {}", startDate, endDate);
        return maintenanceRepository.findByScheduledDateBetweenOrderByScheduledDateAsc(startDate, endDate);
    }

    /**
     * Get maintenance statistics
     * @return Map with statistics
     */
    public Map<String, Long> getMaintenanceStats() {
        log.debug("Calculating maintenance statistics");

        long total = maintenanceRepository.count();
        long scheduled = maintenanceRepository.countByStatus(MaintenanceStatus.SCHEDULED);
        long inProgress = maintenanceRepository.countByStatus(MaintenanceStatus.IN_PROGRESS);
        long completed = maintenanceRepository.countByStatus(MaintenanceStatus.COMPLETED);
        long approved = maintenanceRepository.countByStatus(MaintenanceStatus.APPROVED);
        long cancelled = maintenanceRepository.countByStatus(MaintenanceStatus.CANCELLED);
        long overdue = maintenanceRepository.findOverdueMaintenance(org.springframework.data.domain.PageRequest.of(0, 1000)).getTotalElements();

        return Map.of(
            "total", total,
            "scheduled", scheduled,
            "inProgress", inProgress,
            "completed", completed,
            "approved", approved,
            "cancelled", cancelled,
            "overdue", overdue
        );
    }

    /**
     * Get pending maintenance count for machine
     * @param machineId Machine ID
     * @return Count of pending tasks
     */
    public long getPendingMaintenanceCount(Long machineId) {
        return maintenanceRepository.countByMachineIdAndStatus(machineId, MaintenanceStatus.SCHEDULED)
            + maintenanceRepository.countByMachineIdAndStatus(machineId, MaintenanceStatus.IN_PROGRESS);
    }

    /**
     * Get total count of maintenance tasks
     * @return Total count
     */
    public long getTotalMaintenanceCount() {
        return maintenanceRepository.count();
    }

    /**
     * Check if machine has pending maintenance
     * @param machineId Machine ID
     * @return true if pending maintenance exists
     */
    public boolean hasPendingMaintenance(Long machineId) {
        return getPendingMaintenanceCount(machineId) > 0;
    }
}
