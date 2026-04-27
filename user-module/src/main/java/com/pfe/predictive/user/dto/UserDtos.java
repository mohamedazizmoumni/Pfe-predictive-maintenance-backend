package com.pfe.predictive.user.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.pfe.predictive.core.entity.UserStatus;
import lombok.*;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

/**
 * User DTOs for request/response handling
 *
 * @author User Module
 * @version 1.0
 */

/**
 * CreateUserRequest - For user registration/creation
 * Permission: ADMIN/SUPER_ADMIN
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 100, message = "Username must be 3-100 characters")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 255, message = "Password must be 8+ characters")
    private String password;

    @Size(max = 100, message = "First name max 100 characters")
    private String firstName;

    @Size(max = 100, message = "Last name max 100 characters")
    private String lastName;

    @Size(max = 20, message = "Phone max 20 characters")
    private String phoneNumber;

    @Size(max = 100, message = "Department max 100 characters")
    private String department;

    private List<String> roles; // e.g., ["TECHNICIAN", "MANAGER"]
}


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class UpdateUserRequest {

    @Size(max = 100, message = "First name max 100 characters")
    private String firstName;

    @Size(max = 100, message = "Last name max 100 characters")
    private String lastName;

    @Email(message = "Email must be valid")
    private String email;

    @Size(max = 20, message = "Phone max 20 characters")
    private String phoneNumber;

    @Size(max = 100, message = "Department max 100 characters")
    private String department;
}


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class ChangePasswordRequest {

    @NotBlank(message = "Current password is required")
    private String currentPassword;

    @NotBlank(message = "New password is required")
    @Size(min = 8, max = 255, message = "New password must be 8+ characters")
    private String newPassword;

    @NotBlank(message = "Password confirmation is required")
    private String confirmPassword;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class AssignRoleRequest {

    @NotBlank(message = "Role is required")
    private String role;

    private boolean assign; 
}


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private Long id;

    private String username;

    private String email;

    private String firstName;

    private String lastName;

    private String phoneNumber;

    private String department;

    private UserStatus status;

    private List<String> roles;

    private Boolean mfaEnabled;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastLoginDate;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdDate;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastModifiedDate;

    private String displayName;

    private Boolean locked;
}


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class UserDto {

    private Long id;

    private String username;

    private String email;

    private String displayName;

    private UserStatus status;

    private List<String> roles;
}

/**
 * UserStatsResponse - User management dashboard
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStatsResponse {

    private Long totalUsers;

    private Long activeUsers;

    private Long inactiveUsers;

    private Long lockedUsers;

    private Long adminCount;

    private Long managerCount;

    private Long technicianCount;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastLoginOverall;
}

/**
 * LoginResponse - Return after successful authentication
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private String token;

    private String refreshToken;

    private UserResponse user;

    private long expiresIn; // seconds

    private long refreshExpiresIn; // seconds
}

/**
 * PasswordResetRequest - For password reset flow
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class PasswordResetRequest {

    @Email(message = "Email must be valid")
    @NotBlank(message = "Email is required")
    private String email;
}

/**
 * PasswordResetConfirm - For confirming password reset with token
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class PasswordResetConfirm {

    @NotBlank(message = "Token is required")
    private String token;

    @NotBlank(message = "New password is required")
    @Size(min = 8, max = 255, message = "Password must be 8+ characters")
    private String newPassword;
}
