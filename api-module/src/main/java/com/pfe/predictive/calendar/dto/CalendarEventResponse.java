package com.pfe.predictive.calendar.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Calendar Event Response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalendarEventResponse {

    private Long id;

    private String title;

    private String description;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String eventType;

    private String assignedTo;

    private String status;

    private String priority;

    private String location;

    private Long machineId;

    private Long taskId;

    private String notes;

    private Boolean isAllDay;

    private Boolean isRecurring;

    private String recurrencePattern;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private String createdBy;

    private String updatedBy;
}
