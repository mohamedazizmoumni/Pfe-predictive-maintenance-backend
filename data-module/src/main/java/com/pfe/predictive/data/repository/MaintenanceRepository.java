package com.pfe.predictive.data.repository;

import com.pfe.predictive.core.entity.Maintenance;
import com.pfe.predictive.core.entity.MaintenancePriority;
import com.pfe.predictive.core.entity.MaintenanceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MaintenanceRepository extends JpaRepository<Maintenance, Long> {

    Page<Maintenance> findByStatus(MaintenanceStatus status, Pageable pageable);

    Page<Maintenance> findByMachineId(Long machineId, Pageable pageable);

    @Query("""
        SELECT m FROM Maintenance m
        WHERE (:status IS NULL OR m.status = :status)
          AND (:priority IS NULL OR m.priority = :priority)
    """)
    Page<Maintenance> findByStatusAndPriority(
        @Param("status") MaintenanceStatus status,
        @Param("priority") MaintenancePriority priority,
        Pageable pageable);
}
