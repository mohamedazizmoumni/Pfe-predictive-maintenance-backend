package com.pfe.predictive.core.entity.finance;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A maintenance job rapport submitted by a technician.
 * Requires manager approval, then finance manager approval, before it is considered final.
 */
@Entity
@Table(name = "maintenance_rapports", indexes = {
    @Index(name = "idx_rapport_status",      columnList = "status"),
    @Index(name = "idx_rapport_technician",  columnList = "technician_username"),
    @Index(name = "idx_rapport_machine_id",  columnList = "machine_id"),
    @Index(name = "idx_rapport_created_date", columnList = "created_date")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceRapport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id")
    private Long taskId;

    @Column(name = "machine_id")
    private Long machineId;

    @Column(name = "machine_name", length = 255)
    private String machineName;

    @Column(name = "technician_username", nullable = false, length = 100)
    private String technicianUsername;

    @Column(name = "technician_name", length = 255)
    private String technicianName;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "work_performed", columnDefinition = "TEXT")
    private String workPerformed;

    @Column(name = "parts_replaced", columnDefinition = "TEXT")
    private String partsReplaced;

    @Column(name = "labor_hours", nullable = false)
    @Builder.Default
    private Double laborHours = 0.0;

    @Column(name = "labor_cost", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal laborCost = BigDecimal.ZERO;

    @Column(name = "parts_cost", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal partsCost = BigDecimal.ZERO;

    @Column(name = "total_cost", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal totalCost = BigDecimal.ZERO;

    @ElementCollection
    @CollectionTable(name = "rapport_parts", joinColumns = @JoinColumn(name = "rapport_id"))
    @Builder.Default
    private List<RapportPart> parts = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "rapport_checklist_items", joinColumns = @JoinColumn(name = "rapport_id"))
    @Builder.Default
    private List<ChecklistItem> checklistItems = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private RapportStatus status = RapportStatus.PENDING_MANAGER_APPROVAL;

    @Column(name = "approved_by_manager", length = 100)
    private String approvedByManager;

    @Column(name = "manager_approved_date")
    private LocalDateTime managerApprovedDate;

    @Column(name = "approved_by_finance", length = 100)
    private String approvedByFinance;

    @Column(name = "finance_approved_date")
    private LocalDateTime financeApprovedDate;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @CreationTimestamp
    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @UpdateTimestamp
    @Column(name = "last_modified_date")
    private LocalDateTime lastModifiedDate;

    @Version
    private Long version;
}
