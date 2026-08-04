package com.pfe.predictive.core.entity.portal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * A support request raised by a customer through the portal, tied to one of
 * their linked machines (see CustomerMachineLink). Routed to whichever
 * Manager owns that machine's maintenance today — there's no zone/team
 * concept yet, so "route to the Manager" means "visible to any MANAGER."
 */
@Entity
@Table(name = "support_tickets", indexes = {
    @Index(name = "idx_support_tickets_customer", columnList = "customer_user_id"),
    @Index(name = "idx_support_tickets_machine", columnList = "machine_id"),
    @Index(name = "idx_support_tickets_status", columnList = "status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupportTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_user_id", nullable = false)
    private Long customerUserId;

    @Column(name = "machine_id", nullable = false)
    private Long machineId;

    @Column(nullable = false, length = 255)
    private String subject;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SupportTicketStatus status = SupportTicketStatus.OPEN;

    @Column(name = "assigned_to", length = 100)
    private String assignedTo;

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
