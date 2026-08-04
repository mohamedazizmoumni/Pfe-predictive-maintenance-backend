package com.pfe.predictive.alert.repository;

import com.pfe.predictive.alert.entity.Alert;
import com.pfe.predictive.alert.entity.AlertSeverity;
import com.pfe.predictive.alert.entity.AlertStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * AlertRepository - Spring Data JPA repository for Alert entity.
 *
 * Purpose:
 * - Provide CRUD operations for alerts
 * - Custom query methods for filtering alerts by status, severity, assignee, date
 * - Support for pagination and sorting
 *
 * Key Features:
 * - Find alerts by status (NEW, ACKNOWLEDGED, ESCALATED, CLOSED)
 * - Filter by machine ID, assigned technician, severity
 * - Date-range queries for reporting
 * - Count operations for dashboard metrics
 * - Bulk status updates
 *
 * @author Alert Module
 * @version 1.0
 */
@Repository
public interface AlertRepository extends JpaRepository<Alert, Long>, JpaSpecificationExecutor<Alert> {

    /**
     * Find all alerts with pagination and filtering by status.
     *
     * @param status the alert status filter
     * @param pageable pagination information
     * @return paginated alerts matching status
     */
    Page<Alert> findByStatus(AlertStatus status, Pageable pageable);

    /**
     * Find all alerts for a specific machine, paginated.
     *
     * @param machineId the machine ID
     * @param pageable pagination information
     * @return paginated alerts for the machine
     */
    Page<Alert> findByMachineId(Long machineId, Pageable pageable);

    /**
     * Find alerts assigned to a specific technician, paginated.
     *
     * @param assignedTo the technician username
     * @param pageable pagination information
     * @return paginated alerts assigned to technician
     */
    Page<Alert> findByAssignedTo(String assignedTo, Pageable pageable);

    /**
     * Find alerts by severity level, paginated.
     *
     * @param severity the alert severity
     * @param pageable pagination information
     * @return paginated alerts matching severity
     */
    Page<Alert> findBySeverity(AlertSeverity severity, Pageable pageable);

    /**
     * Find unacknowledged (NEW) alerts for a specific machine.
     *
     * @param machineId the machine ID
     * @param status the alert status (typically NEW)
     * @return list of unacknowledged alerts
     */
    List<Alert> findByMachineIdAndStatus(Long machineId, AlertStatus status);

    /**
     * Find acknowledged but unescalated alerts assigned to a technician.
     *
     * @param assignedTo the technician username
     * @param status the status filter
     * @param pageable pagination information
     * @return paginated alerts
     */
    Page<Alert> findByAssignedToAndStatus(String assignedTo, AlertStatus status, Pageable pageable);

    /**
     * Find alerts by status and severity combination, paginated.
     *
     * @param status the alert status
     * @param severity the alert severity
     * @param pageable pagination information
     * @return paginated alerts matching both filters
     */
    Page<Alert> findByStatusAndSeverity(AlertStatus status, AlertSeverity severity, Pageable pageable);

    /**
     * Find critical unresolved alerts, paginated.
     *
     * @param status the status (usually CRITICAL)
     * @param severity the severity (usually CRITICAL)
     * @param pageable pagination information
     * @return paginated critical alerts
     */
    @Query("SELECT a FROM Alert a WHERE a.status != 'CLOSED' AND a.severity = :severity ORDER BY a.createdDate DESC")
    Page<Alert> findCriticalAlerts(@Param("severity") AlertSeverity severity, Pageable pageable);

    /**
     * Bulk-mark alerts as viewed in a single UPDATE statement, instead of a
     * findById + save round trip per alert (was 2xN queries for N alerts).
     *
     * @param ids the alert IDs to mark as viewed
     * @return number of rows updated
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Alert a SET a.viewed = true WHERE a.id IN :ids")
    int markAsViewedBulk(@Param("ids") List<Long> ids);

    /**
     * Find alerts created within a date range.
     *
     * @param startDate the start date (inclusive)
     * @param endDate the end date (inclusive)
     * @param pageable pagination information
     * @return paginated alerts within date range
     */
    Page<Alert> findByCreatedDateBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    /**
     * Find alerts not yet viewed by assigned technician.
     *
     * @param assignedTo the technician username
     * @param viewed whether the alert has been viewed
     * @param pageable pagination information
     * @return paginated unviewed alerts
     */
    Page<Alert> findByAssignedToAndViewed(String assignedTo, Boolean viewed, Pageable pageable);

    /**
     * Find all alerts for a machine in NEW status (unacknowledged).
     *
     * @param machineId the machine ID
     * @return list of new alerts
     */
    List<Alert> findByMachineIdAndStatusOrderByCreatedDateDesc(Long machineId, AlertStatus status);

    /**
     * Count total unresolved alerts.
     *
     * @return count of alerts not in CLOSED status
     */
    @Query("SELECT COUNT(a) FROM Alert a WHERE a.status != 'CLOSED'")
    long countUnresolvedAlerts();

    /**
     * Count unacknowledged alerts.
     *
     * @return count of NEW status alerts
     */
    long countByStatus(AlertStatus status);

    /**
     * Find paginated alerts with multiple filters (status, machine, severity).
     *
     * @param status filter by status
     * @param machineId filter by machine
     * @param severity filter by severity
     * @param pageable pagination
     * @return paginated matching alerts
     */
    @Query("SELECT a FROM Alert a WHERE a.status = :status AND a.machineId = :machineId AND a.severity = :severity ORDER BY a.createdDate DESC")
    Page<Alert> findByStatusAndMachineIdAndSeverity(
        @Param("status") AlertStatus status,
        @Param("machineId") Long machineId,
        @Param("severity") AlertSeverity severity,
        Pageable pageable
    );

    /**
     * Find alerts assigned to a technician with specific status, ordered by date.
     *
     * @param assignedTo the technician
     * @param status the status filter
     * @param pageable pagination
     * @return paginated alerts
     */
    Page<Alert> findByAssignedToAndStatusOrderByCreatedDateDesc(String assignedTo, AlertStatus status, Pageable pageable);

    /**
     * Find all escalated alerts pending manager review.
     *
     * @param pageable pagination
     * @return paginated escalated alerts
     */
    Page<Alert> findByStatusOrderByEscalatedDateDesc(AlertStatus status, Pageable pageable);

    /**
     * Find alerts closed after a specific date.
     *
     * @param closedDate the cutoff date
     * @param pageable pagination
     * @return paginated recently closed alerts
     */
    Page<Alert> findByClosedDateAfter(LocalDateTime closedDate, Pageable pageable);

    /**
     * Find the currently open incident for a machine+issue combination, if any.
     * Backs the state-based alert dedup check (replaces the old time-based cooldown).
     *
     * @param machineId the machine ID
     * @param issueType the issue/incident type
     * @return the active alert, if one is currently open
     */
    Optional<Alert> findByMachineIdAndIssueTypeAndIsActiveTrue(Long machineId, String issueType);
}
