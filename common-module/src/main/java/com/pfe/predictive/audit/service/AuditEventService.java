package com.pfe.predictive.audit.service;

import com.pfe.predictive.core.entity.AuditEvent;
import com.pfe.predictive.data.repository.AuditEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Writes to the platform-wide audit_events table (see AuditEvent). Failures
 * here are logged and swallowed on purpose — an audit-log write must never
 * be the reason a real user action (creating a user, approving a rapport)
 * fails.
 *
 * This is intentionally called explicitly from the handful of governance
 * actions wired up so far (see UserController), not from a generic
 * interceptor — a blanket "audit every request" approach would bury the
 * meaningful events (who changed what) under routine reads.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditEventService {

    private final AuditEventRepository auditEventRepository;

    public void record(String action, String entityType, Long entityId, String details) {
        record(currentActor(), action, entityType, entityId, details);
    }

    public void record(String actor, String action, String entityType, Long entityId, String details) {
        try {
            AuditEvent event = AuditEvent.builder()
                    .actor(actor != null ? actor : "SYSTEM")
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .details(details)
                    .build();
            auditEventRepository.save(event);
        } catch (Exception ex) {
            log.error("Failed to write audit event action={} entityType={} entityId={}: {}",
                    action, entityType, entityId, ex.getMessage(), ex);
        }
    }

    private String currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null ? authentication.getName() : "SYSTEM";
    }
}
