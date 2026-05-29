package com.pfe.predictive.calendar.mapper;

import com.pfe.predictive.calendar.dto.CalendarEventRequest;
import com.pfe.predictive.calendar.dto.CalendarEventResponse;
import com.pfe.predictive.calendar.entity.CalendarEvent;
import org.springframework.stereotype.Component;

/**
 * Calendar Event Mapper
 */
@Component
public class CalendarEventMapper {

    public CalendarEvent toEntity(CalendarEventRequest request) {
        return CalendarEvent.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .eventType(request.getEventType())
                .assignedTo(request.getAssignedTo())
                .status(request.getStatus() != null ? request.getStatus() : "SCHEDULED")
                .priority(request.getPriority() != null ? request.getPriority() : "MEDIUM")
                .location(request.getLocation())
                .machineId(request.getMachineId())
                .taskId(request.getTaskId())
                .notes(request.getNotes())
                .isAllDay(request.getIsAllDay() != null ? request.getIsAllDay() : false)
                .isRecurring(request.getIsRecurring() != null ? request.getIsRecurring() : false)
                .recurrencePattern(request.getRecurrencePattern())
                .build();
    }

    public CalendarEventResponse toResponse(CalendarEvent entity) {
        return CalendarEventResponse.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .startTime(entity.getStartTime())
                .endTime(entity.getEndTime())
                .eventType(entity.getEventType())
                .assignedTo(entity.getAssignedTo())
                .status(entity.getStatus())
                .priority(entity.getPriority())
                .location(entity.getLocation())
                .machineId(entity.getMachineId())
                .taskId(entity.getTaskId())
                .notes(entity.getNotes())
                .isAllDay(entity.getIsAllDay())
                .isRecurring(entity.getIsRecurring())
                .recurrencePattern(entity.getRecurrencePattern())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .createdBy(entity.getCreatedBy())
                .updatedBy(entity.getUpdatedBy())
                .build();
    }
}
