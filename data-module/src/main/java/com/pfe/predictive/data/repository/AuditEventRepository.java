package com.pfe.predictive.data.repository;

import com.pfe.predictive.core.entity.AuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {

    Page<AuditEvent> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<AuditEvent> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(String entityType, Long entityId, Pageable pageable);

    Page<AuditEvent> findByActorOrderByCreatedAtDesc(String actor, Pageable pageable);

    List<AuditEvent> findByEntityTypeAndEntityIdIn(String entityType, List<Long> entityIds);
}
