package com.pfe.predictive.alert.service;

import com.pfe.predictive.alert.dto.AcknowledgeAlertRequest;
import com.pfe.predictive.alert.dto.CloseAlertRequest;
import com.pfe.predictive.alert.dto.CreateAlertRequest;
import com.pfe.predictive.alert.dto.EscalateAlertRequest;
import com.pfe.predictive.alert.entity.Alert;
import com.pfe.predictive.alert.entity.AlertStatus;
import com.pfe.predictive.alert.mapper.AlertMapper;
import com.pfe.predictive.alert.repository.AlertRepository;
import com.pfe.predictive.alert.service.AlertNotificationProperties;
import com.pfe.predictive.common.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AlertService - Business logic for alert write operations.
 *
 * Purpose:
 * - Create and manage alerts
 * - Handle alert lifecycle transitions (acknowledge, escalate, close)
 * - Enforce business rules and audit trail
 * - Maintain data consistency via @Transactional
 *
 * Responsibility Separation:
 * - AlertService: Write operations, state changes, CRUD
 * - AlertQueryService: Read operations, filtering, reporting (keep separate)
 *
 * Key Operations:
 * - createAlert() - Create new alert
 * - acknowledgeAlert() - Mark as acknowledged by technician
 * - escalateAlert() - Escalate to manager
 * - closeAlert() - Close with resolution notes
 * - markAsViewed() - Track viewing
 *
 * Business Rules:
 * - Only ACKNOWLEDGED alerts can be escalated
 * - Only ESCALATED alerts can be closed
 * - Closing requires resolution notes
 * - Each state change records timestamp and username
 *
 * <300 LOC: Small, focused service with clear responsibilities
 *
 * @author Alert Module
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AlertService {

    private final AlertRepository alertRepository;
    private final AlertMapper alertMapper;
    private final EmailService emailService;
    private final AlertNotificationProperties alertNotificationProperties;
    private final SimpMessagingTemplate messagingTemplate;

    // ============================================================================
    // CREATE OPERATIONS
    // ============================================================================

    /**
     * Create a new alert.
     *
     * @param request the create alert request with alert details
     * @param createdBy the username of who created this alert (from JWT)
     * @return the newly created Alert entity
     */
    public Alert createAlert(CreateAlertRequest request, String createdBy) {
        log.info("Creating new alert for machine {}: {}", request.getMachineId(), request.getTitle());

        Alert alert = alertMapper.toEntity(request, createdBy);
        Alert saved = alertRepository.save(alert);

        log.info("Alert created successfully with ID: {}", saved.getId());

        publishRealtimeAlert(saved);

        // Send notification emails to managers only.
        if (alertNotificationProperties.isEnabled()) {
            emailService.notifyManagersOfMachineAlert(
                    saved.getMachineId(),
                    saved.getSeverity() == null ? "UNKNOWN" : saved.getSeverity().name(),
                    saved.getMessage(),
                    saved.getCreatedDate(),
                    saved.getRecommendations()
            );
            log.info("Alert email dispatch queued for alert {}", saved.getId());
        }
        return saved;
    }

    private void publishRealtimeAlert(Alert alert) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("alertId", alert.getId());
            payload.put("machineId", alert.getMachineId());
            payload.put("title", alert.getTitle());
            payload.put("description", alert.getMessage());
            payload.put("severity", alert.getSeverity() == null ? "UNKNOWN" : alert.getSeverity().name());
            payload.put("status", alert.getStatus() == null ? "UNKNOWN" : alert.getStatus().name());
            payload.put("timestamp", alert.getCreatedDate() == null ? LocalDateTime.now() : alert.getCreatedDate());
            payload.put("recommendedAction", alert.getRecommendations());

            messagingTemplate.convertAndSend("/topic/alerts", payload);
        } catch (Exception ex) {
            log.error("Failed to publish realtime alert {}: {}", alert.getId(), ex.getMessage(), ex);
        }
    }

    // ============================================================================
    // STATE TRANSITION OPERATIONS
    // ============================================================================

    /**
     * Acknowledge an alert (NEW → ACKNOWLEDGED).
     * Typically called by the assigned technician.
     *
     * @param alertId the alert to acknowledge
     * @param acknowledgedBy the technician username
     * @param request acknowledgment details
     * @return the updated alert
     * @throws EntityNotFoundException if alert not found
     */
    public Alert acknowledgeAlert(Long alertId, String acknowledgedBy, AcknowledgeAlertRequest request) {
        log.info("Acknowledging alert {} by technician {}", alertId, acknowledgedBy);

        Alert alert = alertRepository.findById(alertId)
            .orElseThrow(() -> new EntityNotFoundException("Alert not found with ID: " + alertId));

        // Business rule: Only NEW alerts can be acknowledged
        if (alert.getStatus() != AlertStatus.NEW) {
            throw new IllegalStateException(
                "Alert can only be acknowledged from NEW status. Current status: " + alert.getStatus()
            );
        }

        // Map acknowledgment data to entity
        Alert updated = alertMapper.mapAcknowledgment(alert, acknowledgedBy, request);
        Alert saved = alertRepository.save(updated);

        log.info("Alert {} acknowledged at {}", alertId, saved.getAcknowledgedDate());
        return saved;
    }

    /**
     * Escalate an alert (ACKNOWLEDGED → ESCALATED).
     * Typically called by a manager.
     *
     * @param alertId the alert to escalate
     * @param escalatedBy the manager/admin username
     * @param request escalation details (notes, optional reassignment)
     * @return the updated alert
     * @throws EntityNotFoundException if alert not found
     */
    public Alert escalateAlert(Long alertId, String escalatedBy, EscalateAlertRequest request) {
        log.info("Escalating alert {} by manager {}", alertId, escalatedBy);

        Alert alert = alertRepository.findById(alertId)
            .orElseThrow(() -> new EntityNotFoundException("Alert not found with ID: " + alertId));

        if (alert.getStatus() == AlertStatus.CLOSED) {
            throw new IllegalStateException("Closed alerts cannot be escalated");
        }

        if (alert.getStatus() == AlertStatus.ESCALATED) {
            throw new IllegalStateException("Alert is already escalated");
        }

        if (alert.getStatus() == AlertStatus.NEW && alert.getAcknowledgedDate() == null) {
            alert.setAcknowledgedBy(escalatedBy);
            alert.setAcknowledgedDate(LocalDateTime.now());
        }

        // Map escalation data to entity
        Alert updated = alertMapper.mapEscalation(alert, escalatedBy, request);
        Alert saved = alertRepository.save(updated);

        log.info("Alert {} escalated by {} at {}", alertId, escalatedBy, saved.getEscalatedDate());
        return saved;
    }

    /**
     * Close an alert (ESCALATED or NEW → CLOSED).
     * Requires resolution notes. Typically called by manager or admin.
     *
     * @param alertId the alert to close
     * @param closedBy the manager/admin username
     * @param request close request with resolution notes
     * @return the updated alert
     * @throws EntityNotFoundException if alert not found
     */
    public Alert closeAlert(Long alertId, String closedBy, CloseAlertRequest request) {
        log.info("Closing alert {} by {}", alertId, closedBy);

        Alert alert = alertRepository.findById(alertId)
            .orElseThrow(() -> new EntityNotFoundException("Alert not found with ID: " + alertId));

        // Business rule: Only NEW, ACKNOWLEDGED, or ESCALATED alerts can be closed
        if (alert.getStatus() == AlertStatus.CLOSED) {
            throw new IllegalStateException("Alert is already closed");
        }

        // Map closure data to entity
        Alert updated = alertMapper.mapClosure(alert, closedBy, request);
        Alert saved = alertRepository.save(updated);

        log.info("Alert {} closed by {} at {}", alertId, closedBy, saved.getClosedDate());
        return saved;
    }

    // ============================================================================
    // VIEW TRACKING
    // ============================================================================

    /**
     * Mark an alert as viewed when technician opens/reads it.
     * Used to track unread alerts in dashboard.
     *
     * @param alertId the alert to mark as viewed
     */
    public void markAsViewed(Long alertId) {
        alertRepository.findById(alertId).ifPresent(alert -> {
            alert.setViewed(true);
            alertRepository.save(alert);
            log.debug("Alert {} marked as viewed", alertId);
        });
    }

    // ============================================================================
    // BULK OPERATIONS (optional, for dashboard/cleanup)
    // ============================================================================

    /**
     * Bulk acknowledge alerts (e.g., by technician viewing their alert list).
     * Updates viewed flag for efficiency.
     *
     * @param alertIds list of alert IDs to mark as viewed
     * @param viewedBy the user marking as viewed
     * @return count of updated alerts
     */
    @Transactional
    public long markMultipleAsViewed(java.util.List<Long> alertIds, String viewedBy) {
        long count = 0;
        for (Long alertId : alertIds) {
            markAsViewed(alertId);
            count++;
        }
        log.info("Marked {} alerts as viewed by {}", count, viewedBy);
        return count;
    }

    /**
     * Delete an alert (admin/super-admin only, typically for cleanup).
     *
     * @param alertId the alert to delete
     */
    public void deleteAlert(Long alertId) {
        log.warn("Deleting alert {}", alertId);
        alertRepository.deleteById(alertId);
    }

    /**
     * Get alert by ID (internal use, prefer AlertQueryService for API responses).
     *
     * @param alertId the alert ID
     * @return the alert entity
     * @throws EntityNotFoundException if not found
     */
    public Alert getAlertById(Long alertId) {
        return alertRepository.findById(alertId)
            .orElseThrow(() -> new EntityNotFoundException("Alert not found with ID: " + alertId));
    }

}
