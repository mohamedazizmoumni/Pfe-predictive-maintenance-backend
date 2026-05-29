package com.pfe.predictive.calendar.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Calendar Event Request DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalendarEventRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotNull(message = "Start time is required")
    private LocalDateTime startTime;

    @NotNull(message = "End time is required")
    private LocalDateTime endTime;

    @NotBlank(message = "Event type is required")
    private String eventType; // TASK, MEETING, MAINTENANCE, INSPECTION

    @NotBlank(message = "Assigned to is required")
    private String assignedTo;

    private String status; // SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED

    private String priority; // LOW, MEDIUM, HIGH, URGENT

    private String location;

    private Long machineId;

    private Long taskId;

    private String notes;

    private Boolean isAllDay;

    private Boolean isRecurring;

    private String recurrencePattern;
}
