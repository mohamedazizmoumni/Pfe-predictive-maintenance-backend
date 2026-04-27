package com.pfe.predictive.alert.controller;

import com.pfe.predictive.alert.dto.*;
import com.pfe.predictive.alert.entity.AlertSeverity;
import com.pfe.predictive.alert.entity.AlertStatus;
import com.pfe.predictive.alert.mapper.AlertMapper;
import com.pfe.predictive.alert.service.AlertQueryService;
import com.pfe.predictive.alert.service.AlertService;
import com.pfe.predictive.security.PermissionConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * AlertController - REST API endpoints for alert management.
 *
 * Purpose:
 * - Provide REST endpoints for CRUD and lifecycle operations on alerts
 * - Enforce role-based access control via @PreAuthorize(PermissionConstants.PERM_*)
 * - Handle request validation and response transformation
 *
 * Endpoints:
 * - GET /api/v1/alerts - List alerts with filtering and pagination
 * - GET /api/v1/alerts/{id} - Get single alert by ID
 * - GET /api/v1/alerts/stats - Get alert statistics for dashboard
 * - POST /api/v1/alerts - Create new alert (Manager/Admin only)
 * - PUT /api/v1/alerts/{id}/acknowledge - Acknowledge alert (Technician)
 * - PUT /api/v1/alerts/{id}/escalate - Escalate alert (Manager)
 * - PUT /api/v1/alerts/{id}/close - Close alert (Manager/Admin)
 *
 * Role-Based Boundaries:
 * - TECHNICIAN: View assigned alerts, acknowledge only
 * - MANAGER: View all, create, escalate, close
 * - ADMIN/SUPER_ADMIN: Full CRUD
 *
 * Key Features:
 * - Pagination & sorting support on list endpoints
 * - Comprehensive permission checks via PermissionConstants
 * - Audit trail (tracking who did what when)
 * - Error handling with meaningful responses
 * - Request validation via @Valid annotations
 *
 * @author Alert Module
 * @version 1.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;
    private final AlertQueryService alertQueryService;
    private final AlertMapper alertMapper;

    // ============================================================================
    // GET ENDPOINTS (READ)
    // ============================================================================

    /**
     * GET /api/v1/alerts
     * List all alerts with pagination, filtering, and sorting.
     *
     * Query Parameters:
     * - page: page number (0-based, default 0)
     * - size: page size (default 20, max 100)
     * - sort: sort by field (createdDate, severity, status) and direction
     * - status: filter by status (NEW, ACKNOWLEDGED, ESCALATED, CLOSED)
     * - assignedTo: filter by assigned technician
     * - severity: filter by severity (INFO, WARNING, CRITICAL)
     *
     * Permissions:
     * - TECHNICIAN: See only assigned alerts
     * - MANAGER+: See all alerts
     *
     * @param pageable pagination object with page/size/sort info
     * @param authentication Spring Security authentication with username
     * @return paginated list of alerts (AlertResponse)
     */
    @GetMapping
    @PreAuthorize(PermissionConstants.PERM_ALERT_READ)
    public ResponseEntity<Page<AlertResponse>> listAlerts(
        @RequestParam(name = "status", required = false) AlertStatus status,
        @RequestParam(name = "severity", required = false) AlertSeverity severity,
        @RequestParam(name = "assignedTo", required = false) String assignedTo,
        @RequestParam(name = "viewed", required = false) Boolean viewed,
        @RequestParam(name = "search", required = false) String search,
        @PageableDefault(size = 20, page = 0, sort = "createdDate", direction = Sort.Direction.DESC)
        Pageable pageable,
        Authentication authentication) {

        log.info("Listing alerts for user: {}", authentication.getName());

        AlertSearchCriteria filters = AlertSearchCriteria.builder()
            .status(status)
            .severity(severity)
            .assignedTo(normalizeFilter(assignedTo))
            .viewed(viewed)
            .search(normalizeFilter(search))
            .build();

        Page<com.pfe.predictive.alert.entity.Alert> alerts = alertQueryService.searchAlerts(filters, pageable);
        Page<AlertResponse> response = alertMapper.toResponsePage(alerts);

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/alerts/{id}
     * Get a single alert by ID with full details.
     *
     * Permissions:
     * - Creator can view
     * - Assigned technician can view
     * - Manager/Admin can view any
     *
     * @param alertId the alert ID
     * @param authentication Spring Security authentication
     * @return the alert details (AlertResponse)
     */
    @GetMapping("/{id}")
    @PreAuthorize(PermissionConstants.PERM_ALERT_READ)
    public ResponseEntity<AlertResponse> getAlert(
        @PathVariable(name = "id") Long alertId,
        Authentication authentication) {

        log.info("Fetching alert {} by user: {}", alertId, authentication.getName());

        com.pfe.predictive.alert.entity.Alert alert = alertQueryService.getAlertById(alertId);
        AlertResponse response = alertMapper.toResponse(alert);

        // Mark as viewed when technician opens the alert
        alertService.markAsViewed(alertId);

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/alerts/stats
     * Get alert statistics for dashboard/monitoring.
     * Shows counts by status and severity.
     *
     * Permissions:
     * - MANAGER+: Can view overall stats
     * - TECHNICIAN: Can view their own stats
     *
     * @param authentication Spring Security authentication
     * @return alert statistics (AlertStatsResponse)
     */
    @GetMapping("/stats")
    @PreAuthorize(PermissionConstants.PERM_ALERT_READ)
    public ResponseEntity<AlertStatsResponse> getAlertStats(
        Authentication authentication) {

        log.info("Fetching alert statistics for user: {}", authentication.getName());

        AlertQueryService.AlertStatusStats statusStats = alertQueryService.getStatusStats();
        AlertQueryService.AlertSeverityStats severityStats = alertQueryService.getSeverityStats();

        AlertStatsResponse response = AlertStatsResponse.builder()
            .totalAlerts(statusStats.total)
            .newAlerts(statusStats.new_)
            .acknowledgedAlerts(statusStats.acknowledged)
            .escalatedAlerts(statusStats.escalated)
            .closedAlerts(statusStats.closed)
            .criticalCount(severityStats.critical)
            .warningCount(severityStats.warning)
            .infoCount(severityStats.info)
            .unviewedCount(alertQueryService.getUnviewedAlerts(authentication.getName(), Pageable.unpaged()).getTotalElements())
            .build();

        return ResponseEntity.ok(response);
    }


    /**
     * POST /api/v1/alerts
     * Create a new alert for a machine.
     *
     * Request Body:
     * {
     *   "machineId": 1,
     *   "title": "High Vibration Detected",
     *   "message": "Machine 1 vibration exceeded threshold",
     *   "severity": "CRITICAL",
     *   "category": "SENSOR_ANOMALY",
     *   "assignedTo": "technician1",
     *   "recommendations": "Check bearing alignment"
     * }
     *
     * Permissions:
     * - MANAGER+: Can create alerts
     * - TECHNICIAN: Cannot create alerts
     *
     * @param request the create alert request with alert details
     * @param authentication Spring Security authentication (captures createdBy)
     * @return created alert (AlertResponse with ID)
     */
    @PostMapping
    @PreAuthorize(PermissionConstants.PERM_ALERT_CREATE)
    public ResponseEntity<AlertResponse> createAlert(
        @Valid @RequestBody CreateAlertRequest request,
        Authentication authentication) {

        log.info("Creating alert for machine {} by user: {}", request.getMachineId(), authentication.getName());

        com.pfe.predictive.alert.entity.Alert alert = alertService.createAlert(request, authentication.getName());
        AlertResponse response = alertMapper.toResponse(alert);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * PUT /api/v1/alerts/{id}/acknowledge
     * Acknowledge an alert (mark as seen and processed by technician).
     * Transition: NEW → ACKNOWLEDGED
     *
     * Request Body:
     * {
     *   "acknowledgedDate": "2024-03-25T10:30:00"
     * }
     *
     * Permissions:
     * - TECHNICIAN: Can acknowledge assigned alerts
     * - MANAGER+: Can acknowledge any alert
     *
     * @param alertId the alert ID to acknowledge
     * @param request acknowledgment details
     * @param authentication Spring Security authentication
     * @return updated alert (AlertResponse)
     */
    @PutMapping("/{id}/acknowledge")
    @PreAuthorize(PermissionConstants.PERM_ALERT_ACKNOWLEDGE)
    public ResponseEntity<AlertResponse> acknowledgeAlert(
        @PathVariable(name = "id") Long alertId,
        @Valid @RequestBody AcknowledgeAlertRequest request,
        Authentication authentication) {

        log.info("Acknowledging alert {} by user: {}", alertId, authentication.getName());

        com.pfe.predictive.alert.entity.Alert alert = alertService.acknowledgeAlert(
            alertId,
            authentication.getName(),
            request
        );
        AlertResponse response = alertMapper.toResponse(alert);

        return ResponseEntity.ok(response);
    }

    /**
     * PUT /api/v1/alerts/{id}/escalate
     * Escalate an alert for manager attention.
     * Transition: ACKNOWLEDGED → ESCALATED
     * Optional: Reassign to different technician
     *
     * Request Body:
     * {
     *   "escalationNotes": "Unable to resolve, needs manager review",
     *   "reassignTo": "manager1"
     * }
     *
     * Permissions:
     * - TECHNICIAN: Cannot escalate
     * - MANAGER+: Can escalate any alert
     *
     * @param alertId the alert ID to escalate
     * @param request escalation details
     * @param authentication Spring Security authentication
     * @return updated alert (AlertResponse)
     */
    @PutMapping("/{id}/escalate")
    @PreAuthorize(PermissionConstants.PERM_ALERT_ESCALATE)
    public ResponseEntity<AlertResponse> escalateAlert(
        @PathVariable(name = "id") Long alertId,
        @Valid @RequestBody EscalateAlertRequest request,
        Authentication authentication) {

        log.info("Escalating alert {} by user: {}", alertId, authentication.getName());

        com.pfe.predictive.alert.entity.Alert alert = alertService.escalateAlert(
            alertId,
            authentication.getName(),
            request
        );
        AlertResponse response = alertMapper.toResponse(alert);

        return ResponseEntity.ok(response);
    }

    /**
     * PUT /api/v1/alerts/{id}/close
     * Close an alert with resolution notes.
     * Transition: NEW/ACKNOWLEDGED/ESCALATED → CLOSED
     * Requires: Resolution notes explaining how the issue was resolved
     *
     * Request Body:
     * {
     *   "resolutionNotes": "Bearing replaced and tested. Vibration levels normal."
     * }
     *
     * Permissions:
     * - TECHNICIAN: Cannot close
     * - MANAGER+: Can close any alert
     *
     * @param alertId the alert ID to close
     * @param request close request with resolution notes
     * @param authentication Spring Security authentication
     * @return updated alert (AlertResponse)
     */
    @PutMapping("/{id}/close")
    @PreAuthorize(PermissionConstants.PERM_ALERT_CLOSE)
    public ResponseEntity<AlertResponse> closeAlert(
        @PathVariable(name = "id") Long alertId,
        @Valid @RequestBody CloseAlertRequest request,
        Authentication authentication) {

        log.info("Closing alert {} by user: {}", alertId, authentication.getName());

        com.pfe.predictive.alert.entity.Alert alert = alertService.closeAlert(
            alertId,
            authentication.getName(),
            request
        );
        AlertResponse response = alertMapper.toResponse(alert);

        return ResponseEntity.ok(response);
    }

    // ============================================================================
    // DELETE ENDPOINTS
    // ============================================================================

    /**
     * DELETE /api/v1/alerts/{id}
     * Delete an alert (admin/super-admin only).
     *
     * Use Cases:
     * - Clean up false positive alerts
     * - Remove duplicate alerts
     * - System maintenance/cleanup
     *
     * Permissions:
     * - ADMIN/SUPER_ADMIN only
     *
     * @param alertId the alert ID to delete
     * @param authentication Spring Security authentication
     * @return 204 No Content on success
     */
    @DeleteMapping("/{id}")
    @PreAuthorize(PermissionConstants.PERM_ALERT_DELETE)
    public ResponseEntity<Void> deleteAlert(
        @PathVariable(name = "id") Long alertId,
        Authentication authentication) {

        log.warn("Deleting alert {} by user: {}", alertId, authentication.getName());

        alertService.deleteAlert(alertId);
        return ResponseEntity.noContent().build();
    }

    private String normalizeFilter(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
