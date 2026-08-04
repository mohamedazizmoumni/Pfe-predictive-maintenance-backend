package com.pfe.predictive.core.entity.portal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Row-level scoping for the Customer Portal: which machines a given customer
 * (a User with role CUSTOMER) is allowed to see. Every /api/v1/portal/**
 * query must filter through this table — this is the first place in the
 * platform where "authenticated" and "authorized to see this specific
 * machine" genuinely diverge (see PortalMachineController).
 *
 * Plain userId/machineId columns rather than @ManyToOne relations, matching
 * the convention used elsewhere in this codebase (e.g. MaintenanceRapport.machineId)
 * for simplicity across module boundaries.
 */
@Entity
@Table(name = "customer_machine_links", uniqueConstraints = {
    @UniqueConstraint(name = "uq_customer_machine_link", columnNames = {"user_id", "machine_id"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerMachineLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "machine_id", nullable = false)
    private Long machineId;

    @Column(name = "linked_by", length = 100)
    private String linkedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
