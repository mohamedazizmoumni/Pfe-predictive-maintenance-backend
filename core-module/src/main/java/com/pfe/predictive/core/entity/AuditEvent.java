package com.pfe.predictive.core.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Platform-wide, append-only governance log.
 *
 * Distinct from the older, machine-scoped {@link PredictiveActionAudit} table
 * (which requires a non-null machine_id and is unsuitable for events like a
 * role assignment). This entity has no foreign-key coupling to any specific
 * domain object — entityType/entityId are plain descriptive fields, not
 * relations, so writing an audit event can never fail because of a missing
 * or deleted parent row.
 *
 * Rows are never updated or deleted by application code.
 */
@Entity
@Table(name = "audit_events", indexes = {
    @Index(name = "idx_audit_events_actor", columnList = "actor"),
    @Index(name = "idx_audit_events_entity", columnList = "entity_type, entity_id"),
    @Index(name = "idx_audit_events_created_at", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Username of whoever performed the action, or "SYSTEM" for automated actions. */
    @Column(nullable = false, length = 100)
    private String actor;

    /** Short machine-readable verb, e.g. "USER_CREATED", "ROLE_ASSIGNED", "USER_DELETED". */
    @Column(nullable = false, length = 100)
    private String action;

    /** What kind of thing was acted on, e.g. "User", "Machine", "MaintenanceRapport". */
    @Column(name = "entity_type", length = 100)
    private String entityType;

    /** ID of the affected entity, when there is one. */
    @Column(name = "entity_id")
    private Long entityId;

    /** Free-text detail — kept short and human-readable, not a full payload dump. */
    @Column(length = 2000)
    private String details;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
