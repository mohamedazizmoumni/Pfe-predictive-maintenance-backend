package com.pfe.predictive.calendar.service;

import com.pfe.predictive.calendar.dto.CalendarEventRequest;
import com.pfe.predictive.calendar.dto.CalendarEventResponse;
import com.pfe.predictive.calendar.entity.CalendarEvent;
import com.pfe.predictive.calendar.mapper.CalendarEventMapper;
import com.pfe.predictive.calendar.repository.CalendarEventRepository;
import com.pfe.predictive.core.entity.Maintenance;
import com.pfe.predictive.core.entity.User;
import com.pfe.predictive.data.repository.MaintenanceRepository;
import com.pfe.predictive.data.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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
    private final MaintenanceRepository maintenanceRepository;
    private final UserRepository userRepository;

    // These list endpoints have no client-driven paging yet - cap at a
    // generous size (most recent first) instead of loading a machine's or
    // technician's entire lifetime event history.
    private static final int LIST_CAP = 300;
    private static final Sort MOST_RECENT_FIRST = Sort.by(Sort.Direction.DESC, "startTime");

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

        return eventRepository.findAll(PageRequest.of(0, LIST_CAP, MOST_RECENT_FIRST)).getContent().stream()
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

        return eventRepository.findByAssignedTo(technician, PageRequest.of(0, LIST_CAP, MOST_RECENT_FIRST))
                .getContent().stream()
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

        List<CalendarEventResponse> events = new ArrayList<>(
                eventRepository.findByAssignedToAndDateRange(technician, startDate, endDate).stream()
                        .map(eventMapper::toResponse)
                        .toList());

        userRepository.findByUsername(technician).ifPresent(user ->
                events.addAll(toSyntheticEvents(maintenanceRepository
                        .findByAssignedTechnicianIdAndScheduledDateBetween(user.getId(), startDate, endDate),
                        Map.of(user.getId(), user.getUsername()))));

        return sortByStartTime(events);
    }

    /**
     * Get events in date range
     */
    @Transactional(readOnly = true)
    public List<CalendarEventResponse> getEventsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        log.debug("Fetching events between {} and {}", startDate, endDate);

        List<CalendarEventResponse> events = new ArrayList<>(
                eventRepository.findByDateRange(startDate, endDate).stream()
                        .map(eventMapper::toResponse)
                        .toList());

        List<Maintenance> tasks = maintenanceRepository.findByScheduledDateBetween(startDate, endDate);
        events.addAll(toSyntheticEvents(tasks, resolveTechnicianUsernames(tasks)));

        return sortByStartTime(events);
    }

    /**
     * Surfaces a technician's assigned Maintenance work orders as calendar
     * events alongside real CalendarEvent rows — without this, Team Capacity
     * (which only ever read the CalendarEvent table) never showed the same
     * scheduled work a technician's own calendar view already displays
     * (built client-side from their Maintenance tasks), even though both are
     * meant to represent the same underlying schedule.
     *
     * Synthetic IDs are the task's id negated so they can never collide with
     * a real CalendarEvent id (always positive/IDENTITY-generated) — a
     * consumer that tries to PUT/DELETE one of these will get a real 404
     * from the calendar endpoints rather than silently touching a Maintenance
     * row it was never meant to reach.
     */
    private List<CalendarEventResponse> toSyntheticEvents(List<Maintenance> tasks, Map<Long, String> usernamesById) {
        List<CalendarEventResponse> synthetic = new ArrayList<>();
        for (Maintenance task : tasks) {
            if (task.getAssignedTechnicianId() == null) {
                continue;
            }
            String assignedTo = usernamesById.get(task.getAssignedTechnicianId());
            LocalDateTime start = task.getScheduledDate();
            LocalDateTime end = task.getEstimatedDuration() != null
                    ? start.plusMinutes(task.getEstimatedDuration())
                    : start.plusHours(1);

            synthetic.add(CalendarEventResponse.builder()
                    .id(-task.getId())
                    .title(task.getType() + ": " + truncate(task.getDescription(), 80))
                    .description(task.getDescription())
                    .startTime(start)
                    .endTime(end)
                    .eventType("MAINTENANCE_TASK")
                    .assignedTo(assignedTo)
                    .status(task.getStatus() != null ? task.getStatus().name() : null)
                    .priority(task.getPriority() != null ? task.getPriority().name() : null)
                    .machineId(task.getMachineId())
                    .taskId(task.getId())
                    .isAllDay(false)
                    .isRecurring(false)
                    .createdAt(task.getCreatedDate())
                    .updatedAt(task.getLastModifiedDate())
                    .build());
        }
        return synthetic;
    }

    private Map<Long, String> resolveTechnicianUsernames(List<Maintenance> tasks) {
        List<Long> technicianIds = tasks.stream()
                .map(Maintenance::getAssignedTechnicianId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        return userRepository.findAllById(technicianIds).stream()
                .collect(Collectors.toMap(User::getId, User::getUsername));
    }

    private List<CalendarEventResponse> sortByStartTime(List<CalendarEventResponse> events) {
        events.sort(Comparator.comparing(CalendarEventResponse::getStartTime));
        return events;
    }

    private String truncate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        return text.length() <= maxLength ? text : text.substring(0, maxLength - 1) + "…";
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

        return eventRepository.findByMachineId(machineId, PageRequest.of(0, LIST_CAP, MOST_RECENT_FIRST))
                .getContent().stream()
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
