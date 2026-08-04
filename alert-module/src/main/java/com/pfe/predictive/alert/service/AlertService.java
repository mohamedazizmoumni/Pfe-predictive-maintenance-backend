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
import com.pfe.predictive.audit.service.AuditEventService;
import com.pfe.predictive.common.service.AlertEmailContent;
import com.pfe.predictive.common.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pfe.predictive.alert.entity.AlertSeverity;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
    private final AuditEventService auditEventService;

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

        publishRealtimeAlert(saved, "CREATED", AlertBroadcastContext.empty());

        // Send notification emails to managers only.
        if (alertNotificationProperties.isEnabled()) {
            emailService.sendAlertNotification(buildEmailContent(saved, null));
            log.info("Alert email dispatch queued for alert {}", saved.getId());
        }
        return saved;
    }

    // ============================================================================
    // INCIDENT-BASED ALERTING (state-based, replaces time-based cooldown)
    // ============================================================================

    /**
     * Find the currently open incident for a machine+issue combination, if any.
     */
    public Optional<Alert> findActiveAlert(Long machineId, String issueType) {
        return alertRepository.findByMachineIdAndIssueTypeAndIsActiveTrue(machineId, issueType);
    }

    /**
     * Open a new incident: creates the alert, marks it active, sends the email
     * notification, and broadcasts a CREATED event. Only call this when
     * {@link #findActiveAlert} returned empty for this machine+issueType.
     */
    public Alert openIncident(CreateAlertRequest request, String issueType, String createdBy, AlertBroadcastContext context) {
        log.info("Opening new incident for machine {} ({}): {}", request.getMachineId(), issueType, request.getTitle());

        Alert alert = alertMapper.toEntity(request, createdBy);
        alert.setIssueType(issueType);
        alert.setIsActive(true);
        applySnapshot(alert, context);

        Alert saved;
        try {
            saved = alertRepository.save(alert);
        } catch (DataIntegrityViolationException ex) {
            // Lost the race to open this incident: the DB's partial unique
            // index (idx_alerts_active_incident_unique) rejected a second
            // active row for this machine+issueType. Another concurrent
            // caller (the 2s scheduler tick vs. a manual trigger, or two
            // overlapping ticks) already won — return that one instead of
            // duplicating the row, the broadcast, and the email.
            log.info("Incident already active for machine {} ({}) — lost race, reusing existing incident",
                    request.getMachineId(), issueType);
            return alertRepository.findByMachineIdAndIssueTypeAndIsActiveTrue(request.getMachineId(), issueType)
                    .orElseThrow(() -> ex);
        }

        if (alertNotificationProperties.isEnabled()) {
            emailService.sendAlertNotification(buildEmailContent(saved, context));
            saved.setLastEmailSentAt(LocalDateTime.now());
            saved = alertRepository.save(saved);
            log.info("Incident email dispatched for alert {}", saved.getId());
        }

        publishRealtimeAlert(saved, "CREATED", context);
        return saved;
    }

    /**
     * Update the severity/title/message of an already-open incident (e.g. it
     * worsened from WARNING to CRITICAL). Broadcasts an UPDATED event — no email.
     */
    public Alert updateIncidentSeverity(Alert alert, AlertSeverity newSeverity, String newTitle, String newMessage, String newRecommendedAction, AlertBroadcastContext context) {
        alert.setSeverity(newSeverity);
        alert.setTitle(newTitle);
        alert.setMessage(newMessage);
        alert.setRecommendations(newRecommendedAction);
        applySnapshot(alert, context);
        Alert saved = alertRepository.save(alert);

        log.info("Incident {} severity updated to {}", saved.getId(), newSeverity);
        publishRealtimeAlert(saved, "UPDATED", context);
        return saved;
    }

    private void applySnapshot(Alert alert, AlertBroadcastContext context) {
        if (context == null) {
            return;
        }
        alert.setHealthScore(context.health());
        alert.setPredictedRUL(context.predictedRUL());
        alert.setAnomalyProbability(context.anomalyProbability());
        alert.setRiskLevel(context.riskLevel());
        alert.setFailureProbability(context.failureProbability());
        alert.setAnomalyType(context.anomalyType());
        alert.setPredictedFailureType(context.predictedFailureType());
    }

    private AlertEmailContent buildEmailContent(Alert alert, AlertBroadcastContext context) {
        return new AlertEmailContent(
                alert.getMachineId(),
                context != null ? context.machineName() : null,
                alert.getSeverity() == null ? "UNKNOWN" : alert.getSeverity().name(),
                alert.getTitle(),
                alert.getMessage(),
                alert.getRecommendations(),
                alert.getHealthScore(),
                alert.getPredictedRUL(),
                alert.getAnomalyProbability(),
                alert.getRiskLevel(),
                alert.getFailureProbability(),
                alert.getAnomalyType(),
                alert.getPredictedFailureType(),
                alert.getCreatedDate()
        );
    }

    private void publishRealtimeAlert(Alert alert, String eventType, AlertBroadcastContext context) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("alertId", alert.getId());
            payload.put("machineId", alert.getMachineId());
            payload.put("title", alert.getTitle());
            payload.put("description", alert.getMessage());
            payload.put("severity", alert.getSeverity() == null ? "UNKNOWN" : alert.getSeverity().name());
            payload.put("status", alert.getStatus() == null ? "UNKNOWN" : alert.getStatus().name());
            payload.put("eventType", eventType);
            payload.put("timestamp", LocalDateTime.now());
            payload.put("recommendedAction", alert.getRecommendations());

            if (context != null) {
                payload.put("machineName", context.machineName());
                payload.put("health", context.health());
                payload.put("anomalyProbability", context.anomalyProbability());
                payload.put("riskLevel", context.riskLevel());
                payload.put("predictedRUL", context.predictedRUL());
                payload.put("failureProbability", context.failureProbability());
                payload.put("anomalyType", context.anomalyType());
            }

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

        auditEventService.record(escalatedBy, "ALERT_ESCALATED", "Alert", saved.getId(),
                "severity=" + saved.getSeverity());
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

        boolean wasTrackedIncident = alert.getIssueType() != null;

        // Map closure data to entity
        Alert updated = alertMapper.mapClosure(alert, closedBy, request);
        Alert saved = alertRepository.save(updated);

        // Second and last email in the incident's lifecycle: one on open
        // (openIncident), one here on close — never on the noisy severity
        // dips/updates in between (see updateIncidentSeverity, which sends
        // none). Scoped to issueType-tracked incidents (i.e. sensor-anomaly
        // alerts opened by the ML polling loop) so a manually created,
        // one-off alert closure doesn't also fire a "resolved" email it
        // never had an "opened" email to pair with. closeAlert() already
        // rejects an already-CLOSED alert above, so this can only run once.
        if (wasTrackedIncident && alertNotificationProperties.isEnabled()) {
            emailService.sendAlertResolvedNotification(
                    buildEmailContent(saved, null), saved.getResolutionNotes(), closedBy);
            log.info("Resolution email dispatched for alert {}", saved.getId());
        }

        auditEventService.record(closedBy, "ALERT_CLOSED", "Alert", saved.getId(), null);
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
        if (alertIds == null || alertIds.isEmpty()) {
            return 0;
        }
        long count = alertRepository.markAsViewedBulk(alertIds);
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
        auditEventService.record("ALERT_DELETED", "Alert", alertId, null);
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
