package com.pfe.predictive.data.repository.template;

import com.pfe.predictive.core.entity.template.WorkOrderTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkOrderTemplateRepository extends JpaRepository<WorkOrderTemplate, Long> {
    List<WorkOrderTemplate> findByActiveTrue();
}
