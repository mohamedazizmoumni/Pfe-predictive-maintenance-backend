package com.pfe.predictive.data.repository;

import com.pfe.predictive.core.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByEntityTypeAndEntityIdOrderByCreatedAtAsc(String entityType, Long entityId);
    List<Comment> findByEntityTypeAndEntityIdIn(String entityType, List<Long> entityIds);
}
