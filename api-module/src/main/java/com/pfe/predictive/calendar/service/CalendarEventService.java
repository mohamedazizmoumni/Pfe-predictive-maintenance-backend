package com.pfe.predictive.calendar.service;

import com.pfe.predictive.calendar.dto.CalendarEventRequest;
import com.pfe.predictive.calendar.dto.CalendarEventResponse;
import com.pfe.predictive.calendar.entity.CalendarEvent;
import com.pfe.predictive.calendar.mapper.CalendarEventMapper;
import com.pfe.predictive.calendar.repository.CalendarEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Calendar Event Service
 * Manages calendar events for technicians
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CalendarEventService {

    private final CalendarEventRepository eventRepository;
    private final CalendarEventMapper eventMapper;

    /**
     * Create calendar event
     */
    public CalendarEventResponse createEvent(CalendarEventRequest request, String currentUser) {
        log.info("Creating calendar event: {}", request.getTitle());

        CalendarEvent event = eventMapper.toEntity(request);
        event.setCreatedBy(currentUser);
        event.setUpdatedBy(currentUser);

        CalendarEvent saved = eventRepository.save(event);
        log.info("Calendar event created: {}", saved.getId());

        return eventMapper.toResponse(saved);
    }

    /**
     * Get event by ID
     */
    @Transactional(readOnly = true)
    public CalendarEventResponse getEventById(Long id) {
        log.debug("Fetching calendar event: {}", id);

        CalendarEvent event = eventRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Calendar event not found: " + id));

        return eventMapper.toResponse(event);
    }

    /**
     * Get all events
     */
    @Transactional(readOnly = true)
    public List<CalendarEventResponse> getAllEvents() {
        log.debug("Fetching all calendar events");

        return eventRepository.findAll().stream()
                .map(eventMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Update event
     */
    public CalendarEventResponse updateEvent(Long id, CalendarEventRequest request, String currentUser) {
        log.info("Updating calendar event: {}", id);

        CalendarEvent event = eventRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Calendar event not found: " + id));

        event.setTitle(request.getTitle());
        event.setDescription(request.getDescription());
        event.setStartTime(request.getStartTime());
        event.setEndTime(request.getEndTime());
        event.setEventType(request.getEventType());
        event.setAssignedTo(request.getAssignedTo());
        event.setStatus(request.getStatus() != null ? request.getStatus() : event.getStatus());
        event.setPriority(request.getPriority() != null ? request.getPriority() : event.getPriority());
        event.setLocation(request.getLocation());
        event.setMachineId(request.getMachineId());
        event.setTaskId(request.getTaskId());
        event.setNotes(request.getNotes());
        event.setIsAllDay(request.getIsAllDay() != null ? request.getIsAllDay() : event.getIsAllDay());
        event.setIsRecurring(request.getIsRecurring() != null ? request.getIsRecurring() : event.getIsRecurring());
        event.setRecurrencePattern(request.getRecurrencePattern());
        event.setUpdatedBy(currentUser);

        CalendarEvent updated = eventRepository.save(event);
        log.info("Calendar event updated: {}", id);

        return eventMapper.toResponse(updated);
    }

    /**
     * Delete event
     */
    public void deleteEvent(Long id) {
        log.info("Deleting calendar event: {}", id);

        if (!eventRepository.existsById(id)) {
            throw new IllegalArgumentException("Calendar event not found: " + id);
        }

        eventRepository.deleteById(id);
        log.info("Calendar event deleted: {}", id);
    }

    /**
     * Get events for technician
     */
    @Transactional(readOnly = true)
    public List<CalendarEventResponse> getEventsByTechnician(String technician) {
        log.debug("Fetching events for technician: {}", technician);

        return eventRepository.findByAssignedTo(technician).stream()
                .map(eventMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get events for technician in date range
     */
    @Transactional(readOnly = true)
    public List<CalendarEventResponse> getEventsByTechnicianAndDateRange(
            String technician, LocalDateTime startDate, LocalDateTime endDate) {
        log.debug("Fetching events for technician {} between {} and {}", technician, startDate, endDate);

        return eventRepository.findByAssignedToAndDateRange(technician, startDate, endDate).stream()
                .map(eventMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get events in date range
     */
    @Transactional(readOnly = true)
    public List<CalendarEventResponse> getEventsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        log.debug("Fetching events between {} and {}", startDate, endDate);

        return eventRepository.findByDateRange(startDate, endDate).stream()
                .map(eventMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get upcoming events for technician
     */
    @Transactional(readOnly = true)
    public List<CalendarEventResponse> getUpcomingEventsForTechnician(String technician) {
        log.debug("Fetching upcoming events for technician: {}", technician);

        return eventRepository.findUpcomingEventsForTechnician(technician).stream()
                .map(eventMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get today's events for technician
     */
    @Transactional(readOnly = true)
    public List<CalendarEventResponse> getTodayEventsForTechnician(String technician) {
        log.debug("Fetching today's events for technician: {}", technician);

        return eventRepository.findTodayEventsForTechnician(technician).stream()
                .map(eventMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get events by machine
     */
    @Transactional(readOnly = true)
    public List<CalendarEventResponse> getEventsByMachine(Long machineId) {
        log.debug("Fetching events for machine: {}", machineId);

        return eventRepository.findByMachineId(machineId).stream()
                .map(eventMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get events by event type
     */
    @Transactional(readOnly = true)
    public List<CalendarEventResponse> getEventsByType(String eventType) {
        log.debug("Fetching events of type: {}", eventType);

        return eventRepository.findByEventType(eventType).stream()
                .map(eventMapper::toResponse)
                .collect(Collectors.toList());
    }
}
