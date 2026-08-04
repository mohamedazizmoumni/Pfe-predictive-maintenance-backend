package com.pfe.predictive.core.entity.template;

import com.pfe.predictive.core.entity.MaintenancePriority;
import com.pfe.predictive.core.entity.MaintenanceType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * Reusable defaults for creating a Maintenance work order — a Manager/Admin
 * defines one, and it's applied client-side to prefill the maintenance
 * creation form. Not tied to Maintenance by FK; it's a starting point, not a
 * live link.
 */
@Entity
@Table(name = "work_order_templates")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkOrderTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MaintenanceType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MaintenancePriority priority;

    private Integer estimatedDuration;

    @Column(length = 2000)
    private String defaultNotes;

    @Column(nullable = false)
    private boolean active;

    private String createdBy;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @UpdateTimestamp
    private LocalDateTime lastModifiedDate;
}
