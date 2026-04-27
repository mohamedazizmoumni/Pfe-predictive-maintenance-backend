package com.pfe.predictive.alert.service;

import com.pfe.predictive.alert.dto.AlertSearchCriteria;
import com.pfe.predictive.alert.entity.Alert;
import com.pfe.predictive.alert.entity.AlertSeverity;
import com.pfe.predictive.alert.entity.AlertStatus;
import com.pfe.predictive.alert.repository.AlertRepository;
import com.pfe.predictive.alert.repository.AlertSpecifications;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * AlertQueryService - Business logic for alert read operations.
 *
 * Purpose:
 * - Provide filtered alert queries with pagination
 * - Support dashboard queries and reporting
 * - Read-only operations with @Transactional(readOnly=true) for performance
 * - Central location for all alert querying logic
 *
 * Responsibility Separation:
 * - AlertQueryService: Read operations, filtering, pagination, reporting
 * - AlertService: Write operations, state changes, CRUD
 *
 * Key Operations:
 * - getAlertById() - Get single alert by ID
 * - getAllAlerts() - Get all alerts with pagination
 * - getAlertsByStatus() - Filter by status
 * - getAlertsByMachine() - Filter by machine
 * - getAlertsByAssignee() - Filter by assigned technician
 * - getCriticalAlerts() - Get critical severity alerts
 * - getDashboardStats() - Get summary statistics
 *
 * <300 LOC: Small, focused query service
 *
 * @author Alert Module
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AlertQueryService {

    private final AlertRepository alertRepository;

    // ============================================================================
    // SINGLE ALERT QUERIES
    // ============================================================================

    /**
     * Get alert by ID.
     *
     * @param alertId the alert ID
     * @return the alert entity
     * @throws EntityNotFoundException if not found
     */
    public Alert getAlertById(Long alertId) {
        return alertRepository.findById(alertId)
            .orElseThrow(() -> new EntityNotFoundException("Alert not found with ID: " + alertId));
    }

    // ============================================================================
    // LIST & FILTER QUERIES
    // ============================================================================

    /**
     * Get all alerts with pagination and sorting.
     *
     * @param pageable pagination info (page, size, sort)
     * @return paginated alerts
     */
    public Page<Alert> getAllAlerts(Pageable pageable) {
        log.debug("Fetching all alerts with pagination: {}", pageable);
        return alertRepository.findAll(pageable);
    }

    /**
     * Search alerts with the flexible filter set exposed by the frontend table.
     *
     * @param criteria optional filters
     * @param pageable pagination config
     * @return paginated alerts satisfying the filters
     */
    public Page<Alert> searchAlerts(AlertSearchCriteria criteria, Pageable pageable) {
        Specification<Alert> specification = AlertSpecifications.withFilters(criteria);
        return alertRepository.findAll(specification, pageable);
    }

    /**
     * Get alerts filtered by status.
     *
     * @param status the alert status (NEW, ACKNOWLEDGED, ESCALATED, CLOSED)
     * @param pageable pagination info
     * @return paginated alerts matching status
     */
    public Page<Alert> getAlertsByStatus(AlertStatus status, Pageable pageable) {
        log.debug("Fetching alerts with status: {}", status);
        return alertRepository.findByStatus(status, pageable);
    }

    /**
     * Get alerts for a specific machine.
     *
     * @param machineId the machine ID
     * @param pageable pagination info
     * @return paginated alerts for the machine
     */
    public Page<Alert> getAlertsByMachine(Long machineId, Pageable pageable) {
        log.debug("Fetching alerts for machine: {}", machineId);
        return alertRepository.findByMachineId(machineId, pageable);
    }

    /**
     * Get alerts assigned to a specific technician.
     *
     * @param assignedTo the technician username
     * @param pageable pagination info
     * @return paginated alerts assigned to technician
     */
    public Page<Alert> getAlertsByAssignee(String assignedTo, Pageable pageable) {
        log.debug("Fetching alerts assigned to: {}", assignedTo);
        return alertRepository.findByAssignedTo(assignedTo, pageable);
    }

    /**
     * Get alerts by severity level.
     *
     * @param severity the severity (INFO, WARNING, CRITICAL)
     * @param pageable pagination info
     * @return paginated alerts matching severity
     */
    public Page<Alert> getAlertsBySeverity(AlertSeverity severity, Pageable pageable) {
        log.debug("Fetching alerts with severity: {}", severity);
        return alertRepository.findBySeverity(severity, pageable);
    }

    /**
     * Get unacknowledged (NEW) alerts for a machine.
     *
     * @param machineId the machine ID
     * @return list of new alerts
     */
    public List<Alert> getUnacknowledgedAlertsForMachine(Long machineId) {
        log.debug("Fetching unacknowledged alerts for machine: {}", machineId);
        return alertRepository.findByMachineIdAndStatus(machineId, AlertStatus.NEW);
    }

    /**
     * Get alerts assigned to technician with specific status.
     *
     * @param assignedTo the technician username
     * @param status the status filter
     * @param pageable pagination info
     * @return paginated alerts
     */
    public Page<Alert> getAlertsByAssigneeAndStatus(String assignedTo, AlertStatus status, Pageable pageable) {
        log.debug("Fetching alerts for {} with status {}", assignedTo, status);
        return alertRepository.findByAssignedToAndStatusOrderByCreatedDateDesc(assignedTo, status, pageable);
    }

    // ============================================================================
    // CRITICAL ALERTS
    // ============================================================================

    /**
     * Get critical severity alerts that are unresolved.
     *
     * @param pageable pagination info
     * @return paginated critical unresolved alerts
     */
    public Page<Alert> getCriticalAlerts(Pageable pageable) {
        log.debug("Fetching critical alerts");
        return alertRepository.findCriticalAlerts(AlertSeverity.CRITICAL, pageable);
    }

    /**
     * Get critical alerts for a specific machine.
     *
     * @param machineId the machine ID
     * @return list of critical alerts for machine
     */
    public List<Alert> getCriticalAlertsForMachine(Long machineId) {
        return alertRepository.findByMachineIdAndStatusOrderByCreatedDateDesc(machineId, AlertStatus.NEW);
    }

    // ============================================================================
    // UNVIEWED ALERTS
    // ============================================================================

    /**
     * Get unviewed (unread) alerts for a technician.
     * Used for "new messages" badge on dashboard.
     *
     * @param assignedTo the technician username
     * @param pageable pagination info
     * @return paginated unviewed alerts
     */
    public Page<Alert> getUnviewedAlerts(String assignedTo, Pageable pageable) {
        log.debug("Fetching unviewed alerts for: {}", assignedTo);
        return alertRepository.findByAssignedToAndViewed(assignedTo, false, pageable);
    }

    // ============================================================================
    // DATE RANGE QUERIES
    // ============================================================================

    /**
     * Get alerts created within a date range.
     *
     * @param startDate the start date (inclusive)
     * @param endDate the end date (inclusive)
     * @param pageable pagination info
     * @return paginated alerts within range
     */
    public Page<Alert> getAlertsByDateRange(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        log.debug("Fetching alerts between {} and {}", startDate, endDate);
        return alertRepository.findByCreatedDateBetween(startDate, endDate, pageable);
    }

    /**
     * Get recently closed alerts (for monitoring resolution time).
     *
     * @param hoursBack the number of hours to look back
     * @param pageable pagination info
     * @return paginated recently closed alerts
     */
    public Page<Alert> getRecentlyClosedAlerts(int hoursBack, Pageable pageable) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusHours(hoursBack);
        log.debug("Fetching alerts closed after: {}", cutoffDate);
        return alertRepository.findByClosedDateAfter(cutoffDate, pageable);
    }

    // ============================================================================
    // STATISTICS & REPORTING
    // ============================================================================

    /**
     * Count unresolved alerts (NEW, ACKNOWLEDGED, ESCALATED).
     *
     * @return count of unresolved alerts
     */
    public long countUnresolvedAlerts() {
        return alertRepository.countUnresolvedAlerts();
    }

    /**
     * Count alerts by status.
     *
     * @param status the alert status
     * @return count of alerts with that status
     */
    public long countByStatus(AlertStatus status) {
        return alertRepository.countByStatus(status);
    }

    /**
     * Get alert statistics by status for dashboard.
     * Returns counts: NEW, ACKNOWLEDGED, ESCALATED, CLOSED
     *
     * @return object with status counts
     */
    public AlertStatusStats getStatusStats() {
        return AlertStatusStats.builder()
            .new_(countByStatus(AlertStatus.NEW))
            .acknowledged(countByStatus(AlertStatus.ACKNOWLEDGED))
            .escalated(countByStatus(AlertStatus.ESCALATED))
            .closed(countByStatus(AlertStatus.CLOSED))
            .total(countByStatus(AlertStatus.NEW)
                + countByStatus(AlertStatus.ACKNOWLEDGED)
                + countByStatus(AlertStatus.ESCALATED)
                + countByStatus(AlertStatus.CLOSED))
            .build();
    }

    /**
     * Get alert statistics by severity for dashboard.
     *
     * @return object with severity counts
     */
    public AlertSeverityStats getSeverityStats() {
        return AlertSeverityStats.builder()
            .info(alertRepository.findBySeverity(AlertSeverity.INFO, Pageable.unpaged()).getTotalElements())
            .warning(alertRepository.findBySeverity(AlertSeverity.WARNING, Pageable.unpaged()).getTotalElements())
            .critical(alertRepository.findBySeverity(AlertSeverity.CRITICAL, Pageable.unpaged()).getTotalElements())
            .build();
    }

    // ============================================================================
    // HELPER CLASSES
    // ============================================================================

    /**
     * Status statistics DTO (internal use)
     */
    public static class AlertStatusStats {
        public Long new_;
        public Long acknowledged;
        public Long escalated;
        public Long closed;
        public Long total;

        public static AlertStatusStatsBuilder builder() {
            return new AlertStatusStatsBuilder();
        }

        public static class AlertStatusStatsBuilder {
            private Long new_;
            private Long acknowledged;
            private Long escalated;
            private Long closed;
            private Long total;

            public AlertStatusStatsBuilder new_(Long new_) {
                this.new_ = new_;
                return this;
            }

            public AlertStatusStatsBuilder acknowledged(Long acknowledged) {
                this.acknowledged = acknowledged;
                return this;
            }

            public AlertStatusStatsBuilder escalated(Long escalated) {
                this.escalated = escalated;
                return this;
            }

            public AlertStatusStatsBuilder closed(Long closed) {
                this.closed = closed;
                return this;
            }

            public AlertStatusStatsBuilder total(Long total) {
                this.total = total;
                return this;
            }

            public AlertStatusStats build() {
                AlertStatusStats stats = new AlertStatusStats();
                stats.new_ = this.new_;
                stats.acknowledged = this.acknowledged;
                stats.escalated = this.escalated;
                stats.closed = this.closed;
                stats.total = this.total;
                return stats;
            }
        }
    }

    /**
     * Severity statistics DTO (internal use)
     */
    public static class AlertSeverityStats {
        public Long info;
        public Long warning;
        public Long critical;

        public static AlertSeverityStatsBuilder builder() {
            return new AlertSeverityStatsBuilder();
        }

        public static class AlertSeverityStatsBuilder {
            private Long info;
            private Long warning;
            private Long critical;

            public AlertSeverityStatsBuilder info(Long info) {
                this.info = info;
                return this;
            }

            public AlertSeverityStatsBuilder warning(Long warning) {
                this.warning = warning;
                return this;
            }

            public AlertSeverityStatsBuilder critical(Long critical) {
                this.critical = critical;
                return this;
            }

            public AlertSeverityStats build() {
                AlertSeverityStats stats = new AlertSeverityStats();
                stats.info = this.info;
                stats.warning = this.warning;
                stats.critical = this.critical;
                return stats;
            }
        }
    }
}
