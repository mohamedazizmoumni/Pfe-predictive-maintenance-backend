package com.pfe.predictive.core.entity.template;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Spawns a new Maintenance work order for a machine every intervalDays, using
 * a WorkOrderTemplate for the work-order defaults. Not an FK relation to
 * WorkOrderTemplate/Maintenance — plain id columns, consistent with the rest
 * of the codebase's "generic id reference" convention (AuditEvent, Attachment).
 */
@Entity
@Table(name = "recurring_maintenance_rules")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecurringMaintenanceRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long machineId;

    @Column(nullable = false)
    private Long workOrderTemplateId;

    @Column(nullable = false)
    private Integer intervalDays;

    private Long assignedTechnicianId;

    @Column(nullable = false)
    private LocalDateTime nextRunDate;

    private Long lastGeneratedMaintenanceId;

    @Column(nullable = false)
    private boolean active;

    private String createdBy;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @UpdateTimestamp
    private LocalDateTime lastModifiedDate;
}
