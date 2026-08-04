package com.pfe.predictive.data.repository;

import com.pfe.predictive.core.entity.Maintenance;
import com.pfe.predictive.core.entity.MaintenancePriority;
import com.pfe.predictive.core.entity.MaintenanceStatus;
import com.pfe.predictive.core.entity.MaintenanceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MaintenanceRepository extends JpaRepository<Maintenance, Long> {

    Page<Maintenance> findByStatus(MaintenanceStatus status, Pageable pageable);

    Page<Maintenance> findByMachineId(Long machineId, Pageable pageable);

    List<Maintenance> findByStatusInAndScheduledDateBefore(List<MaintenanceStatus> statuses, LocalDateTime cutoff);

    // Reliability analytics (MTBF/MTTR/root-cause) source data — see ReliabilityService.
    List<Maintenance> findByTypeInAndCompletedDateIsNotNullOrderByCompletedDateAsc(List<MaintenanceType> types);

    List<Maintenance> findByTypeInAndCompletedDateIsNotNullOrderByCompletedDateDesc(List<MaintenanceType> types);

    List<Maintenance> findByMachineIdAndTypeInAndCompletedDateIsNotNullOrderByCompletedDateDesc(
        Long machineId, List<MaintenanceType> types);

    // Manager capacity-planning source data — see CapacityService.
    List<Maintenance> findByAssignedTechnicianIdAndStatusIn(Long technicianId, List<MaintenanceStatus> statuses);

    // Calendar unification — see CalendarEventService. A technician's assigned
    // work orders are scheduled work same as any CalendarEvent row, but were
    // never surfaced through /calendar/events/*, so Team Capacity (which only
    // reads CalendarEvent) never showed them even though a technician's own
    // calendar view (built client-side from Maintenance) did.
    List<Maintenance> findByScheduledDateBetween(LocalDateTime start, LocalDateTime end);

    List<Maintenance> findByAssignedTechnicianIdAndScheduledDateBetween(Long technicianId, LocalDateTime start, LocalDateTime end);

    long countByStatusIn(List<MaintenanceStatus> statuses);

    @Query("""
        SELECT m FROM Maintenance m
        WHERE (:status IS NULL OR m.status = :status)
          AND (:priority IS NULL OR m.priority = :priority)
          AND (:assignedTechnicianId IS NULL OR m.assignedTechnicianId = :assignedTechnicianId)
    """)
    Page<Maintenance> findByStatusAndPriority(
        @Param("status") MaintenanceStatus status,
        @Param("priority") MaintenancePriority priority,
        @Param("assignedTechnicianId") Long assignedTechnicianId,
        Pageable pageable);
}
