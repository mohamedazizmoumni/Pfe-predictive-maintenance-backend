package com.pfe.predictive.calendar.repository;

import com.pfe.predictive.calendar.entity.CalendarEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Calendar Event Repository
 */
@Repository
public interface CalendarEventRepository extends JpaRepository<CalendarEvent, Long> {

    /**
     * Find events by assigned technician
     */
    List<CalendarEvent> findByAssignedTo(String assignedTo);

    /**
     * Find events by assigned technician and date range
     */
    @Query("SELECT e FROM CalendarEvent e WHERE e.assignedTo = :assignedTo " +
           "AND e.startTime >= :startDate AND e.endTime <= :endDate " +
           "ORDER BY e.startTime ASC")
    List<CalendarEvent> findByAssignedToAndDateRange(
            @Param("assignedTo") String assignedTo,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Find events by date range
     */
    @Query("SELECT e FROM CalendarEvent e WHERE e.startTime >= :startDate " +
           "AND e.endTime <= :endDate ORDER BY e.startTime ASC")
    List<CalendarEvent> findByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Find events by machine
     */
    List<CalendarEvent> findByMachineId(Long machineId);

    /**
     * Find events by task
     */
    List<CalendarEvent> findByTaskId(Long taskId);

    /**
     * Find events by status
     */
    List<CalendarEvent> findByStatus(String status);

    /**
     * Find events by event type
     */
    List<CalendarEvent> findByEventType(String eventType);

    /**
     * Find upcoming events for technician
     */
    @Query("SELECT e FROM CalendarEvent e WHERE e.assignedTo = :assignedTo " +
           "AND e.startTime >= CURRENT_TIMESTAMP " +
           "AND e.status != 'CANCELLED' " +
           "ORDER BY e.startTime ASC")
    List<CalendarEvent> findUpcomingEventsForTechnician(@Param("assignedTo") String assignedTo);

    /**
     * Find today's events for technician
     */
    @Query("SELECT e FROM CalendarEvent e WHERE e.assignedTo = :assignedTo " +
           "AND CAST(e.startTime AS date) = CAST(CURRENT_TIMESTAMP AS date) " +
           "ORDER BY e.startTime ASC")
    List<CalendarEvent> findTodayEventsForTechnician(@Param("assignedTo") String assignedTo);
}
