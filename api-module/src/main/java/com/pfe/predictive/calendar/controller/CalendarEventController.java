package com.pfe.predictive.calendar.controller;

import com.pfe.predictive.calendar.dto.CalendarEventRequest;
import com.pfe.predictive.calendar.dto.CalendarEventResponse;
import com.pfe.predictive.calendar.service.CalendarEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Calendar Event Controller
 * Manages calendar events for technicians
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/calendar/events")
@RequiredArgsConstructor
@Tag(name = "Calendar", description = "Calendar and scheduling management for technicians")
public class CalendarEventController {

    private final CalendarEventService eventService;

    /**
     * Get all calendar events
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Get all calendar events")
    public ResponseEntity<List<CalendarEventResponse>> getAllEvents() {
        log.info("Fetching all calendar events");
        List<CalendarEventResponse> events = eventService.getAllEvents();
        return ResponseEntity.ok(events);
    }

    /**
     * Get event by ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Get calendar event by ID")
    public ResponseEntity<CalendarEventResponse> getEventById(@PathVariable Long id) {
        log.info("Fetching calendar event: {}", id);
        CalendarEventResponse event = eventService.getEventById(id);
        return ResponseEntity.ok(event);
    }

    /**
     * Create calendar event
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Create calendar event")
    public ResponseEntity<CalendarEventResponse> createEvent(
            @Valid @RequestBody CalendarEventRequest request,
            Authentication authentication) {
        log.info("Creating calendar event: {}", request.getTitle());
        String currentUser = authentication != null ? authentication.getName() : "SYSTEM";
        CalendarEventResponse event = eventService.createEvent(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(event);
    }

    /**
     * Update calendar event
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Update calendar event")
    public ResponseEntity<CalendarEventResponse> updateEvent(
            @PathVariable Long id,
            @Valid @RequestBody CalendarEventRequest request,
            Authentication authentication) {
        log.info("Updating calendar event: {}", id);
        String currentUser = authentication != null ? authentication.getName() : "SYSTEM";
        CalendarEventResponse event = eventService.updateEvent(id, request, currentUser);
        return ResponseEntity.ok(event);
    }

    /**
     * Delete calendar event
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Delete calendar event")
    public ResponseEntity<Void> deleteEvent(@PathVariable Long id) {
        log.info("Deleting calendar event: {}", id);
        eventService.deleteEvent(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get events for technician
     */
    @GetMapping("/technician/{technician}")
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Get events for technician")
    public ResponseEntity<List<CalendarEventResponse>> getEventsByTechnician(@PathVariable String technician) {
        log.info("Fetching events for technician: {}", technician);
        List<CalendarEventResponse> events = eventService.getEventsByTechnician(technician);
        return ResponseEntity.ok(events);
    }

    /**
     * Get upcoming events for technician
     */
    @GetMapping("/technician/{technician}/upcoming")
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Get upcoming events for technician")
    public ResponseEntity<List<CalendarEventResponse>> getUpcomingEventsForTechnician(@PathVariable String technician) {
        log.info("Fetching upcoming events for technician: {}", technician);
        List<CalendarEventResponse> events = eventService.getUpcomingEventsForTechnician(technician);
        return ResponseEntity.ok(events);
    }

    /**
     * Get today's events for technician
     */
    @GetMapping("/technician/{technician}/today")
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Get today's events for technician")
    public ResponseEntity<List<CalendarEventResponse>> getTodayEventsForTechnician(@PathVariable String technician) {
        log.info("Fetching today's events for technician: {}", technician);
        List<CalendarEventResponse> events = eventService.getTodayEventsForTechnician(technician);
        return ResponseEntity.ok(events);
    }

    /**
     * Get events for technician in date range
     */
    @GetMapping("/technician/{technician}/range")
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Get events for technician in date range")
    public ResponseEntity<List<CalendarEventResponse>> getEventsByTechnicianAndDateRange(
            @PathVariable String technician,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        log.info("Fetching events for technician {} between {} and {}", technician, startDate, endDate);
        List<CalendarEventResponse> events = eventService.getEventsByTechnicianAndDateRange(technician, startDate, endDate);
        return ResponseEntity.ok(events);
    }

    /**
     * Get events in date range
     */
    @GetMapping("/range")
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Get events in date range")
    public ResponseEntity<List<CalendarEventResponse>> getEventsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        log.info("Fetching events between {} and {}", startDate, endDate);
        List<CalendarEventResponse> events = eventService.getEventsByDateRange(startDate, endDate);
        return ResponseEntity.ok(events);
    }

    /**
     * Get events by machine
     */
    @GetMapping("/machine/{machineId}")
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Get events by machine")
    public ResponseEntity<List<CalendarEventResponse>> getEventsByMachine(@PathVariable Long machineId) {
        log.info("Fetching events for machine: {}", machineId);
        List<CalendarEventResponse> events = eventService.getEventsByMachine(machineId);
        return ResponseEntity.ok(events);
    }

    /**
     * Get events by type
     */
    @GetMapping("/type/{eventType}")
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Get events by type")
    public ResponseEntity<List<CalendarEventResponse>> getEventsByType(@PathVariable String eventType) {
        log.info("Fetching events of type: {}", eventType);
        List<CalendarEventResponse> events = eventService.getEventsByType(eventType);
        return ResponseEntity.ok(events);
    }
}
