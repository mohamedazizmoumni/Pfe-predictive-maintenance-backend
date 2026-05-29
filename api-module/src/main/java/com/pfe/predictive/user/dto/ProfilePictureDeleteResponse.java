package com.pfe.predictive.user.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDateTime;

/**
 * ProfilePictureDeleteResponse - Response after deleting profile picture
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfilePictureDeleteResponse {

    private Long userId;

    private String message;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime deletedAt;
}
