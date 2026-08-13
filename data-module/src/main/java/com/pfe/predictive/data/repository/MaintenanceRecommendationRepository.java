package com.pfe.predictive.data.repository;

import com.pfe.predictive.core.entity.MaintenanceRecommendation;
import com.pfe.predictive.core.entity.MaintenanceRecommendationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MaintenanceRecommendationRepository extends JpaRepository<MaintenanceRecommendation, Long> {
    Page<MaintenanceRecommendation> findAllByOrderByGeneratedAtDesc(Pageable pageable);
    Page<MaintenanceRecommendation> findByStatusOrderByGeneratedAtDesc(MaintenanceRecommendationStatus status, Pageable pageable);

    // Guards the auto-generation path (a new HIGH/CRITICAL incident) against
    // piling up duplicate PENDING recommendations for a machine that's
    // already awaiting a manager's decision.
    boolean existsByMachineIdAndStatus(Long machineId, MaintenanceRecommendationStatus status);

    // Digital Machine Passport (Priority 3): this machine's recommendation history.
    java.util.List<MaintenanceRecommendation> findByMachineIdOrderByGeneratedAtDesc(Long machineId);

    // Maintenance Intervention Report (Priority 5): the recommendation that
    // created a given work order, if it was AI-triggered rather than manual.
    java.util.Optional<MaintenanceRecommendation> findByResultingMaintenanceId(Long resultingMaintenanceId);
}
