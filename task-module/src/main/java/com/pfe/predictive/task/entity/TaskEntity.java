package com.pfe.predictive.task.entity;

import lombok.*;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tasks")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Task {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status; // PENDING, IN_PROGRESS, COMPLETED, CANCELLED

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskPriority priority; // LOW, MEDIUM, HIGH, CRITICAL

    @Column(name = "machine_id")
    private Long machineId;

    @Column(name = "maintenance_id")
    private Long maintenanceId;

    @Column(length = 100)
    private String assignedTo;

    @Column
    private LocalDateTime dueDate;

    @Column
    private LocalDateTime completedDate;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @UpdateTimestamp
    @Column
    private LocalDateTime lastModifiedDate;

    @Version
    private Long version;
}

public enum TaskStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}

public enum TaskPriority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}
