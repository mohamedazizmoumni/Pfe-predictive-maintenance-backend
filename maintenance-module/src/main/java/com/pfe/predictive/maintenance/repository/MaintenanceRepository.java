package com.pfe.predictive.maintenance.repository;

import com.pfe.predictive.core.entity.Maintenance;
import com.pfe.predictive.core.entity.MaintenanceStatus;
import com.pfe.predictive.core.entity.MaintenanceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Maintenance Repository
 * Handles all database operations for maintenance tasks and schedules
 *
 * @author Maintenance Module
 * @version 1.0
 */
@Repository
public interface MaintenanceRepository extends JpaRepository<Maintenance, Long> {

    /**
     * Find maintenance by machine ID with pagination
     */
    Page<Maintenance> findByMachineId(Long machineId, Pageable pageable);

    /**
     * Find by status with pagination
     */
    Page<Maintenance> findByStatus(MaintenanceStatus status, Pageable pageable);

    /**
     * Find by maintenance type
     */
    Page<Maintenance> findByType(MaintenanceType type, Pageable pageable);

    /**
     * Find by assigned technician
     */
    Page<Maintenance> findByAssignedTechnicianId(Long technicianId, Pageable pageable);

    /**
     * Find pending maintenance (SCHEDULED or IN_PROGRESS)
     */
    @Query("SELECT m FROM Maintenance m WHERE m.status IN ('SCHEDULED', 'IN_PROGRESS') ORDER BY m.scheduledDate ASC")
    Page<Maintenance> findPendingMaintenance(Pageable pageable);

    /**
     * Find overdue maintenance
     */
    @Query("SELECT m FROM Maintenance m WHERE m.status = 'SCHEDULED' AND m.scheduledDate < CURRENT_TIMESTAMP ORDER BY m.scheduledDate ASC")
    Page<Maintenance> findOverdueMaintenance(Pageable pageable);

    /**
     * Find maintenance scheduled between dates
     */
    List<Maintenance> findByScheduledDateBetweenOrderByScheduledDateAsc(LocalDateTime start, LocalDateTime end);

    /**
     * Find completed maintenance for machine
     */
    @Query("SELECT m FROM Maintenance m WHERE m.machineId = :machineId AND m.status = 'COMPLETED' ORDER BY m.completedDate DESC")
    Page<Maintenance> findCompletedMaintenanceForMachine(@Param("machineId") Long machineId, Pageable pageable);

    /**
     * Count maintenance by status
     */
    long countByStatus(MaintenanceStatus status);

    /**
     * Count maintenance by machine and status
     */
    long countByMachineIdAndStatus(Long machineId, MaintenanceStatus status);

    /**
     * Find maintenance requiring approval
     */
    @Query("SELECT m FROM Maintenance m WHERE m.status = 'COMPLETED' AND m.approvedDate IS NULL ORDER BY m.completedDate ASC")
    Page<Maintenance> findPendingApproval(Pageable pageable);

    /**
     * Find maintenance by machine status
     */
    List<Maintenance> findByMachineIdAndStatusOrderByScheduledDateAsc(Long machineId, MaintenanceStatus status);
}
