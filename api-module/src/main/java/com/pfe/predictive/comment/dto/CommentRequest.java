package com.pfe.predictive.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CommentRequest {
    @NotBlank
    private String entityType;
    @NotNull
    private Long entityId;
    @NotBlank
    private String body;
}
