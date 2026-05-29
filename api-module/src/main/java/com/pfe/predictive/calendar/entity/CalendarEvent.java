package com.pfe.predictive.calendar.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Calendar Event Entity
 * Tracks tasks, meetings, and maintenance schedules for technicians
 */
@Entity
@Table(name = "calendar_events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalendarEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private LocalDateTime endTime;

    @Column(length = 100)
    private String eventType; // TASK, MEETING, MAINTENANCE, INSPECTION

    @Column(length = 100)
    private String assignedTo; // Technician username

    @Column(length = 50)
    private String status; // SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED

    @Column(length = 50)
    private String priority; // LOW, MEDIUM, HIGH, URGENT

    @Column(length = 255)
    private String location;

    @Column(name = "machine_id")
    private Long machineId;

    @Column(name = "task_id")
    private Long taskId;

    @Column(length = 1000)
    private String notes;

    @Column
    @Builder.Default
    private Boolean isAllDay = false;

    @Column
    @Builder.Default
    private Boolean isRecurring = false;

    @Column(length = 50)
    private String recurrencePattern; // DAILY, WEEKLY, MONTHLY, YEARLY

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column
    private LocalDateTime updatedAt;

    @Column(length = 100)
    private String createdBy;

    @Column(length = 100)
    private String updatedBy;
}
