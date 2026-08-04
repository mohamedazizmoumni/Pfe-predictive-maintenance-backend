package com.pfe.predictive.data.repository.template;

import com.pfe.predictive.core.entity.template.RecurringMaintenanceRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RecurringMaintenanceRuleRepository extends JpaRepository<RecurringMaintenanceRule, Long> {
    List<RecurringMaintenanceRule> findByMachineId(Long machineId);
    List<RecurringMaintenanceRule> findByActiveTrueAndNextRunDateLessThanEqual(LocalDateTime now);
}
