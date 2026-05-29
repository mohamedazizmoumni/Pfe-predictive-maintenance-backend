package com.pfe.predictive.user.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

/**
 * ProfileResponse - Complete user profile information
 * Returned when fetching user profile
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileResponse {

    private Long id;

    private String username; // read-only

    @Email(message = "Email must be valid")
    private String email;

    @Size(min = 2, max = 100, message = "First name must be 2-100 characters")
    private String firstName;

    @Size(min = 2, max = 100, message = "Last name must be 2-100 characters")
    private String lastName;

    @Size(max = 150, message = "Display name max 150 characters")
    private String displayName;

    @Size(max = 20, message = "Phone max 20 characters")
    private String phoneNumber;

    @Size(max = 100, message = "Department max 100 characters")
    private String department;

    private String status; // ACTIVE, INACTIVE, LOCKED

    private Boolean mfaEnabled;

    private String profilePictureUrl;

    private List<String> roles;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastLoginDate;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdDate;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastModifiedDate;
}
