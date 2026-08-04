package com.pfe.predictive.core.entity.portal;

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
 * A message on a support-ticket thread — the "communicate with maintenance
 * managers" portal capability, deliberately scoped to a ticket rather than a
 * free-standing conversation, so every message has a clear subject and owner.
 */
@Entity
@Table(name = "portal_messages", indexes = {
    @Index(name = "idx_portal_messages_ticket", columnList = "ticket_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortalMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_id", nullable = false)
    private Long ticketId;

    @Column(name = "sender_username", nullable = false, length = 100)
    private String senderUsername;

    /** True when the sender is the customer; false when it's a Manager/Admin reply. Drives message alignment in the UI. */
    @Column(name = "from_customer", nullable = false)
    private boolean fromCustomer;

    @Column(nullable = false, length = 2000)
    private String body;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
