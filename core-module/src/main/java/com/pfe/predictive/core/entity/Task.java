package com.pfe.predictive.core.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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

    @Column(length = 50)
    @Builder.Default
    private String status = "PENDING"; 

    @Column(length = 50)
    @Builder.Default
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

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column
    private LocalDateTime updatedAt;
}
