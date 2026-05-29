package com.pfe.predictive.user.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Profile DTOs for profile management feature
 *
 * @author User Module
 * @version 1.0
 */

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

/**
 * UpdateProfileRequest - For updating user profile information
 * Does NOT include username (read-only) or password (use separate endpoint)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {

    @Size(min = 2, max = 100, message = "First name must be 2-100 characters")
    private String firstName;

    @Size(min = 2, max = 100, message = "Last name must be 2-100 characters")
    private String lastName;

    @Email(message = "Email must be valid")
    private String email;

    @Size(max = 20, message = "Phone max 20 characters")
    private String phoneNumber;

    @Size(max = 100, message = "Department max 100 characters")
    private String department;

    @Size(max = 150, message = "Display name max 150 characters")
    private String displayName;
}

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

/**
 * ProfileValidationError - Validation error response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileValidationError {

    private String field;

    private String message;

    private Object rejectedValue;
}
