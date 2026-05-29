package com.pfe.predictive.user.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDateTime;

/**
 * ProfilePictureResponse - Response after uploading profile picture
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfilePictureResponse {

    private Long userId;

    private String profilePictureUrl;

    private String message;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime uploadedAt;
}
