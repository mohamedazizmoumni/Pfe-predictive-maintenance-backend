package com.pfe.predictive.audit.controller;

import com.pfe.predictive.core.entity.AuditEvent;
import com.pfe.predictive.data.repository.AuditEventRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only view of the audit_events log (see AuditEventService for writes).
 * Super Admin / Admin only — this is a governance surface, not a general
 * activity feed.
 */
@RestController
@RequestMapping("/api/v1/audit-events")
@RequiredArgsConstructor
@Tag(name = "Audit Events", description = "Platform-wide governance audit log (read-only)")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class AuditEventController {

    private final AuditEventRepository auditEventRepository;

    @GetMapping
    @Operation(summary = "List audit events", description = "Most recent first; optionally filter by entity type + id")
    public ResponseEntity<Page<AuditEvent>> listAuditEvents(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) Long entityId,
            @PageableDefault(size = 50) Pageable pageable) {

        if (entityType != null && entityId != null) {
            return ResponseEntity.ok(
                    auditEventRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(entityType, entityId, pageable));
        }
        return ResponseEntity.ok(auditEventRepository.findAllByOrderByCreatedAtDesc(pageable));
    }
}
