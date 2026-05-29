package com.pfe.predictive.core.entity.finance;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents an expense report submitted by any authenticated user.
 * Finance managers can approve or reject pending reports.
 */
@Entity
@Table(name = "expense_reports", indexes = {
    @Index(name = "idx_expense_status",       columnList = "status"),
    @Index(name = "idx_expense_category",     columnList = "category"),
    @Index(name = "idx_expense_submitted_by", columnList = "submitted_by"),
    @Index(name = "idx_expense_machine_id",   columnList = "machine_id"),
    @Index(name = "idx_expense_created_date", columnList = "created_date")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ExpenseCategory category;

    @Column(name = "machine_id")
    private Long machineId;

    @Column(name = "machine_name", length = 255)
    private String machineName;

    @Column(name = "maintenance_task_id")
    private Long maintenanceTaskId;

    @Column(name = "submitted_by", nullable = false, length = 100)
    private String submittedBy;

    @Column(name = "submitted_by_name", length = 255)
    private String submittedByName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ExpenseStatus status = ExpenseStatus.PENDING;

    @Column(name = "reviewed_by", length = 100)
    private String reviewedBy;

    @Column(name = "reviewed_date")
    private LocalDateTime reviewedDate;

    @Column(name = "review_note", columnDefinition = "TEXT")
    private String reviewNote;

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
