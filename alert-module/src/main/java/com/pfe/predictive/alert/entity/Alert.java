package com.pfe.predictive.alert.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "alerts", indexes = {
    @Index(name = "idx_machine_id", columnList = "machine_id"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_assigned_to", columnList = "assigned_to"),
    @Index(name = "idx_created_date", columnList = "created_date"),
    @Index(name = "idx_severity", columnList = "severity")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "machine_id", nullable = false)
    private Long machineId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(length = 2000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertStatus status;

    @Enumerated(EnumType.STRING)
    private AlertCategory category;

    @Column(length = 100)
    private String sourceReference;

    @Column(nullable = false)
    private Boolean viewed = false;

    @Column(name = "assigned_to", length = 100)
    private String assignedTo;

    @Column(length = 100)
    private String createdBy;

    @Column(length = 100)
    private String acknowledgedBy;

    private LocalDateTime acknowledgedDate;

    @Column(length = 100)
    private String escalatedBy;

    private LocalDateTime escalatedDate;

    @Column(name = "escalation_notes", length = 1000)
    private String escalationNotes;

    @Column(length = 100)
    private String closedBy;

    private LocalDateTime closedDate;

    @Column(length = 1000)
    private String resolutionNotes;

    @Column(length = 1000)
    private String recommendations;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @UpdateTimestamp
    private LocalDateTime lastModifiedDate;

    @Version
    private Long version;

    public boolean isActionable() {
        return status != AlertStatus.CLOSED;
    }

    public boolean isNew() {
        return status == AlertStatus.NEW && Boolean.FALSE.equals(viewed);
    }

    public String getStatusDisplay() {
        return switch (status) {
            case NEW -> "New (Not Acknowledged)";
            case ACKNOWLEDGED -> "Acknowledged";
            case ESCALATED -> "Escalated";
            case CLOSED -> "Closed";
        };
    }

    public String getSeverityBadgeColor() {
        return switch (severity) {
            case INFO -> "blue";
            case WARNING -> "yellow";
            case CRITICAL -> "red";
        };
    }
}
