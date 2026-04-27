package com.pfe.predictive.machine.repository;

import com.pfe.predictive.machine.entity.PredictiveActionAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PredictiveActionAuditRepository extends JpaRepository<PredictiveActionAudit, Long> {
}
