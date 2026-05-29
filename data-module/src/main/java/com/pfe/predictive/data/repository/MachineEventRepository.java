package com.pfe.predictive.data.repository;

import com.pfe.predictive.core.entity.MachineEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for MachineEvent - Real-time event detection storage.
 */
@Repository
public interface MachineEventRepository extends JpaRepository<MachineEvent, Long> {
    
    /**
     * Find all events for a machine.
     */
    List<MachineEvent> findByMachineIdOrderByEventTimestampDesc(Long machineId);
    
    /**
     * Find events for a machine within a time range.
     */
    List<MachineEvent> findByMachineIdAndEventTimestampBetween(
        Long machineId,
        LocalDateTime start,
        LocalDateTime end
    );
    
    /**
     * Find unacknowledged events for a machine.
     */
    List<MachineEvent> findByMachineIdAndAcknowledgedFalseOrderByEventTimestampDesc(Long machineId);
    
    /**
     * Find events by type.
     */
    List<MachineEvent> findByEventTypeOrderByEventTimestampDesc(String eventType);
    
    /**
     * Find events by severity.
     */
    List<MachineEvent> findBySeverityOrderByEventTimestampDesc(String severity);
    
    /**
     * Find critical unacknowledged events.
     */
    @Query("SELECT e FROM MachineEvent e WHERE e.severity = 'CRITICAL' AND e.acknowledged = FALSE ORDER BY e.eventTimestamp DESC")
    List<MachineEvent> findCriticalUnacknowledgedEvents();
    
    /**
     * Count unacknowledged events for a machine.
     */
    long countByMachineIdAndAcknowledgedFalse(Long machineId);
    
    /**
     * Count events by type for a machine.
     */
    long countByMachineIdAndEventType(Long machineId, String eventType);
    
    /**
     * Find recent events (last 24 hours).
     */
    @Query("SELECT e FROM MachineEvent e WHERE e.eventTimestamp > :since ORDER BY e.eventTimestamp DESC")
    List<MachineEvent> findRecentEvents(@Param("since") LocalDateTime since);
    
    /**
     * Find recent events for a machine.
     */
    @Query("SELECT e FROM MachineEvent e WHERE e.machineId = :machineId AND e.eventTimestamp > :since ORDER BY e.eventTimestamp DESC")
    List<MachineEvent> findRecentEventsByMachine(@Param("machineId") Long machineId, @Param("since") LocalDateTime since);
}
