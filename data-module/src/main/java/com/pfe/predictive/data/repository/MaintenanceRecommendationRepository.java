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
}
