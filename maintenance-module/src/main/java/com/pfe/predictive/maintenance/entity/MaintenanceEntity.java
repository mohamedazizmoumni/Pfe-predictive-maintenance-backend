package com.pfe.predictive.maintenance.entity;

import lombok.*;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Maintenance Entity - Work orders for machine maintenance
 */
@Entity
@Table(name = "maintenances")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Maintenance {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "machine_id", nullable = false)
    private Long machineId; // Reference to Machine entity (in machine-module)

    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private MaintenanceType type; // PREVENTIVE, CORRECTIVE, PREDICTIVE

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private MaintenanceStatus status; // PLANNED, IN_PROGRESS, COMPLETED, CANCELLED

    @Column(nullable = false)
    private LocalDateTime scheduledDate;

    @Column
    private LocalDateTime startDate;

    @Column
    private LocalDateTime completedDate;

    @Column
    private Integer estimatedHours;

    @Column
    private Integer actualHours;

    @Column(length = 100)
    private String assignedTo; // Technician username

    @Column(length = 1000)
    private String notes;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @UpdateTimestamp
    @Column
    private LocalDateTime lastModifiedDate;

    @Version
    private Long version;
}

public enum MaintenanceType {
    PREVENTIVE,
    CORRECTIVE,
    PREDICTIVE
}

public enum MaintenanceStatus {
    PLANNED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}
