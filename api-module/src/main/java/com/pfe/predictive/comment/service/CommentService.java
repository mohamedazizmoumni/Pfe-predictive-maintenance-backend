package com.pfe.predictive.comment.service;

import com.pfe.predictive.audit.service.AuditEventService;
import com.pfe.predictive.comment.dto.CommentRequest;
import com.pfe.predictive.comment.dto.CommentResponse;
import com.pfe.predictive.common.exception.ResourceNotFoundException;
import com.pfe.predictive.core.entity.Comment;
import com.pfe.predictive.data.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentService {

    public static final List<String> ALLOWED_ENTITY_TYPES = List.of("MACHINE", "MAINTENANCE");

    private final CommentRepository commentRepository;
    private final AuditEventService auditEventService;

    public CommentResponse add(CommentRequest request, String author) {
        if (!ALLOWED_ENTITY_TYPES.contains(request.getEntityType())) {
            throw new IllegalArgumentException("Unsupported entityType: " + request.getEntityType());
        }
        Comment comment = Comment.builder()
                .entityType(request.getEntityType())
                .entityId(request.getEntityId())
                .authorUsername(author)
                .body(request.getBody())
                .build();
        Comment saved = commentRepository.save(comment);
        auditEventService.record(author, "COMMENT_ADDED", request.getEntityType(), request.getEntityId(), null);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> list(String entityType, Long entityId) {
        return commentRepository.findByEntityTypeAndEntityIdOrderByCreatedAtAsc(entityType, entityId).stream()
                .map(this::toResponse)
                .toList();
    }

    public void delete(Long id, String requester, boolean isPrivileged) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found: " + id));
        if (!isPrivileged && !comment.getAuthorUsername().equals(requester)) {
            throw new AccessDeniedException("Cannot delete another user's comment");
        }
        commentRepository.delete(comment);
    }

    private CommentResponse toResponse(Comment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .entityType(comment.getEntityType())
                .entityId(comment.getEntityId())
                .authorUsername(comment.getAuthorUsername())
                .body(comment.getBody())
                .createdAt(comment.getCreatedAt())
                .build();
    }
}
