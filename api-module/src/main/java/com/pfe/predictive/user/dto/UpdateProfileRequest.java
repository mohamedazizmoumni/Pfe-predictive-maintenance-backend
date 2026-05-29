package com.pfe.predictive.user.dto;

import lombok.*;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

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
