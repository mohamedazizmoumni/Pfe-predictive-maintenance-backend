package com.pfe.predictive.comment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponse {
    private Long id;
    private String entityType;
    private Long entityId;
    private String authorUsername;
    private String body;
    private LocalDateTime createdAt;
}
