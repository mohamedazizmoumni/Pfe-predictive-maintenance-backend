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
public class TaskEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(length = 1000)
    private String description;

    @Column(length = 50)
    private String status = "PENDING"; 

    @Column(length = 50)
    private String priority = "MEDIUM"; 

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

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column
    private LocalDateTime updatedAt;
}
