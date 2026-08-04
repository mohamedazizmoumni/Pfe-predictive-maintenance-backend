package com.pfe.predictive.data.repository;

import com.pfe.predictive.core.entity.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

    List<Attachment> findByEntityTypeAndEntityIdOrderByUploadedAtDesc(String entityType, Long entityId);
}
