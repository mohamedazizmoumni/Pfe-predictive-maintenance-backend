package com.pfe.predictive.user.controller;

import com.pfe.predictive.audit.service.AuditEventService;
import com.pfe.predictive.auth.client.FaceRecognitionClient;
import com.pfe.predictive.common.service.EmailService;
import com.pfe.predictive.core.entity.Role;
import com.pfe.predictive.core.entity.User;
import com.pfe.predictive.data.repository.RoleRepository;
import com.pfe.predictive.data.repository.UserRepository;
import com.pfe.predictive.user.dto.CreateUserRequest;
import com.pfe.predictive.user.dto.UpdateUserRequest;
import com.pfe.predictive.user.dto.UserDTO;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final FaceRecognitionClient faceRecognitionClient;
    private final AuditEventService auditEventService;
    private final EmailService emailService;

    public UserController(UserRepository userRepository,
                          RoleRepository roleRepository,
                          PasswordEncoder passwordEncoder,
                          FaceRecognitionClient faceRecognitionClient,
                          AuditEventService auditEventService,
                          EmailService emailService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.faceRecognitionClient = faceRecognitionClient;
        this.auditEventService = auditEventService;
        this.emailService = emailService;
    }

    // -------------------------------------------------------------------------
    // READ — accessible to any authenticated user.
    // -------------------------------------------------------------------------

    @GetMapping
    public ResponseEntity<List<UserDTO>> getUsers(@RequestParam(value = "role", required = false) String role) {
        List<User> users = userRepository.findAll();

        if (role != null && !role.isBlank()) {
            String normalizedRole = role.toUpperCase();
            users = users.stream()
                    .filter(user -> user.getRoles() != null && user.getRoles().stream()
                            .map(Role::getName)
                            .map(String::toUpperCase)
                            .anyMatch(name -> name.equals(normalizedRole) || name.equals("ROLE_" + normalizedRole)))
                    .toList();
        }

        List<UserDTO> response = users.stream()
                .sorted(Comparator.comparing(User::getId, Comparator.nullsLast(Long::compareTo)))
                .map(this::toDto)
                .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{idOrUsername}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable String idOrUsername) {
        try {
            Long id = Long.parseLong(idOrUsername);
            return userRepository.findById(id)
                    .map(user -> ResponseEntity.ok(toDto(user)))
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (NumberFormatException e) {
            return userRepository.findByUsername(idOrUsername)
                    .map(user -> ResponseEntity.ok(toDto(user)))
                    .orElseGet(() -> ResponseEntity.notFound().build());
        }
    }

    // -------------------------------------------------------------------------
    // CREATE — admin only.
    // This is the ONLY way to create new accounts.
    // Self-registration via /auth/register and /auth/signup is disabled.
    // -------------------------------------------------------------------------

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<?> createUser(@Valid @RequestBody CreateUserRequest request) {
        try {
            User user = findUpdatableUser(request.getUsername(), request.getEmail());
            boolean isNewUser = user == null;

            if (isNewUser) {
                if (request.getUsername() == null || request.getUsername().isBlank()) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Username is required"));
                }
                if (request.getEmail() == null || request.getEmail().isBlank()) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Email is required"));
                }

                user = new User();
                user.setPassword(passwordEncoder.encode(resolvePassword(request.getPassword())));
                user.setStatus("ACTIVE");
                user.setLocked(false);
                user.setMfaEnabled(false);
                // New admin-created accounts start with no face enrolled
                user.setFaceEnrolled(false);
            } else if (request.getPassword() != null && !request.getPassword().isBlank()) {
                user.setPassword(passwordEncoder.encode(request.getPassword()));
            }

            user.setUsername(request.getUsername());
            user.setEmail(request.getEmail());
            user.setFirstName(request.getFirstName());
            user.setLastName(request.getLastName());
            user.setDepartment(request.getDepartment());
            user.setPhoneNumber(request.getPhoneNumber());
            user.setRoles(resolveRoles(request.getRoles()));

            User saved = userRepository.save(user);
            auditEventService.record(
                    isNewUser ? "USER_CREATED" : "USER_UPDATED",
                    "User", saved.getId(),
                    "roles=" + rolesOf(saved));

            return ResponseEntity.status(isNewUser ? 201 : 200).body(toDto(saved));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    // -------------------------------------------------------------------------
    // UPDATE — admins can update anyone; any authenticated user can update
    // their own profile (name/email/phone/department only — see the
    // ownership check below, which blocks a self-edit from touching
    // username, status/lock, or roles).
    // -------------------------------------------------------------------------

    @PutMapping("/{idOrUsername}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updateUser(@PathVariable String idOrUsername,
                                        @Valid @RequestBody UpdateUserRequest request,
                                        org.springframework.security.core.Authentication authentication) {
        try {
            User user;
            try {
                Long id = Long.parseLong(idOrUsername);
                user = userRepository.findById(id)
                        .orElseThrow(() -> new EntityNotFoundException("User not found"));
            } catch (NumberFormatException e) {
                user = userRepository.findByUsername(idOrUsername)
                        .orElseThrow(() -> new EntityNotFoundException("User not found: " + idOrUsername));
            }

            boolean isPrivileged = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
                            || a.getAuthority().equals("ROLE_SUPER_ADMIN"));
            boolean isSelf = user.getUsername().equalsIgnoreCase(authentication.getName());

            if (!isPrivileged && !isSelf) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "You can only update your own profile");
            }
            if (!isPrivileged) {
                // Self-service edit: block the privileged-only fields even if a
                // crafted request includes them — the profile page only ever
                // sends name/email/phone/department, but a non-admin caller must
                // never be able to grant themselves roles, unlock/relock their
                // own account, or change their username through this endpoint.
                boolean touchesUsername = request.getUsername() != null && !request.getUsername().isBlank();
                boolean touchesStatus = request.getStatus() != null && !request.getStatus().isBlank();
                boolean touchesRoles = request.getRoles() != null;
                if (touchesUsername || touchesStatus || touchesRoles) {
                    throw new org.springframework.security.access.AccessDeniedException(
                            "You are not allowed to change your username, status, or roles");
                }
            }

            if (request.getUsername() != null && !request.getUsername().isBlank()) {
                if (!request.getUsername().equalsIgnoreCase(user.getUsername())
                        && userRepository.existsByUsername(request.getUsername())) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Username already exists"));
                }
                user.setUsername(request.getUsername());
            }

            if (request.getEmail() != null && !request.getEmail().isBlank()) {
                if (!request.getEmail().equalsIgnoreCase(user.getEmail())
                        && userRepository.existsByEmail(request.getEmail())) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Email already exists"));
                }
                user.setEmail(request.getEmail());
            }

            if (request.getFirstName()   != null) user.setFirstName(request.getFirstName());
            if (request.getLastName()    != null) user.setLastName(request.getLastName());
            if (request.getPhoneNumber() != null) user.setPhoneNumber(request.getPhoneNumber());
            if (request.getDepartment()  != null) user.setDepartment(request.getDepartment());

            // Password reset — admin/super-admin only. There is no
            // self-service password change, so this field is silently
            // ignored (not rejected) for a non-privileged self-edit rather
            // than erroring, matching how username/status/roles are handled
            // above for the same caller.
            boolean passwordReset = false;
            if (isPrivileged && request.getPassword() != null && !request.getPassword().isBlank()) {
                user.setPassword(passwordEncoder.encode(request.getPassword()));
                passwordReset = true;
            }

            if (request.getStatus() != null && !request.getStatus().isBlank()) {
                user.setStatus(request.getStatus());
                if ("LOCKED".equalsIgnoreCase(request.getStatus())) {
                    user.setLocked(true);
                } else if ("ACTIVE".equalsIgnoreCase(request.getStatus())
                        || "INACTIVE".equalsIgnoreCase(request.getStatus())) {
                    user.setLocked(false);
                }
            }

            List<String> previousRoles = rolesOf(user);
            boolean rolesChanged = false;
            if (request.getRoles() != null) {
                user.setRoles(resolveRoles(request.getRoles()));
                rolesChanged = !previousRoles.equals(rolesOf(user));
            }

            User saved = userRepository.save(user);
            if (rolesChanged) {
                auditEventService.record(
                        "ROLE_ASSIGNED",
                        "User", saved.getId(),
                        "roles changed from " + previousRoles + " to " + rolesOf(saved));
            }
            if (passwordReset) {
                auditEventService.record("PASSWORD_RESET", "User", saved.getId(),
                        "password reset by " + authentication.getName());
                emailService.sendPasswordResetNotification(
                        saved.getEmail(), saved.getFirstName(), authentication.getName());
            }

            return ResponseEntity.ok(toDto(saved));
        } catch (EntityNotFoundException ex) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    // -------------------------------------------------------------------------
    // DELETE — admin only.
    // -------------------------------------------------------------------------

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        Optional<User> user = userRepository.findById(id);
        if (user.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        String username = user.get().getUsername();
        userRepository.deleteById(id);
        auditEventService.record("USER_DELETED", "User", id, "username=" + username);
        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------------------------------
    // FACE ENROLLMENT RESET — admin only.
    //
    // Clears face_enrolled and face_enrolled_at so the user is prompted to
    // re-enroll on their next login.  Optionally also removes the stored
    // embedding from the ML service (best-effort; never fails the request).
    //
    // DELETE /api/v1/users/{id}/face-enrollment
    // -------------------------------------------------------------------------

    @DeleteMapping("/{id}/face-enrollment")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
    @Transactional
    public ResponseEntity<?> resetFaceEnrollment(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + id));

        if (!user.isFaceEnrolled()) {
            return ResponseEntity.ok(Map.of(
                    "message", "User has no face enrollment to reset.",
                    "userId",  id,
                    "faceEnrolled", false
            ));
        }

        // Best-effort: remove embedding from ML service
        try {
            faceRecognitionClient.deleteEnrollment(id);
        } catch (Exception ex) {
            // Non-fatal — the DB reset proceeds regardless
            org.slf4j.LoggerFactory.getLogger(UserController.class)
                    .warn("Could not remove ML embedding for user {} (non-fatal): {}", id, ex.getMessage());
        }

        user.setFaceEnrolled(false);
        user.setFaceEnrolledAt(null);
        // Clear profile picture set during enrollment so it is re-captured
        user.setProfilePictureUrl(null);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
                "message",      "Face enrollment reset. User will be prompted to re-enroll on next login.",
                "userId",       id,
                "username",     user.getUsername(),
                "faceEnrolled", false
        ));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private UserDTO toDto(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setDisplayName(user.getDisplayName());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setDepartment(user.getDepartment());
        dto.setStatus(user.getStatus());
        dto.setMfaEnabled(user.getMfaEnabled());
        dto.setLocked(user.isLocked());
        dto.setCreatedDate(user.getCreatedDate());
        dto.setProfilePictureUrl(user.getProfilePictureUrl());
        dto.setFaceEnrolled(user.isFaceEnrolled());
        dto.setFaceEnrolledAt(user.getFaceEnrolledAt());
        dto.setRoles(user.getRoles() == null
                ? List.of()
                : user.getRoles().stream().map(Role::getName).sorted().toList());
        return dto;
    }

    private Set<Role> resolveRoles(List<String> requestedRoles) {
        Set<Role> resolvedRoles = new LinkedHashSet<>();

        if (requestedRoles == null || requestedRoles.isEmpty()) {
            addRoleByName(resolvedRoles, "TECHNICIAN");
            return resolvedRoles;
        }

        for (String roleName : requestedRoles) {
            if (roleName == null || roleName.isBlank()) continue;

            String normalized = roleName.trim().toUpperCase();
            if (normalized.startsWith("ROLE_")) normalized = normalized.substring(5);
            addRoleByName(resolvedRoles, normalized);
        }

        if (resolvedRoles.isEmpty()) addRoleByName(resolvedRoles, "TECHNICIAN");

        return resolvedRoles;
    }

    private List<String> rolesOf(User user) {
        return user.getRoles() == null
                ? List.of()
                : user.getRoles().stream().map(Role::getName).sorted().toList();
    }

    private void addRoleByName(Set<Role> roles, String roleName) {
        roleRepository.findByName(roleName)
                .or(() -> roleRepository.findByName("ROLE_" + roleName))
                .ifPresentOrElse(roles::add, () -> {
                    throw new IllegalArgumentException("Unknown role: " + roleName);
                });
    }

    private User findUpdatableUser(String username, String email) {
        if (username != null && !username.isBlank()) {
            return userRepository.findByUsername(username).orElseGet(() -> {
                if (email != null && !email.isBlank()) {
                    return userRepository.findByEmail(email).orElse(null);
                }
                return null;
            });
        }
        if (email != null && !email.isBlank()) {
            return userRepository.findByEmail(email).orElse(null);
        }
        return null;
    }

    private String resolvePassword(String rawPassword) {
        return (rawPassword == null || rawPassword.isBlank()) ? "ChangeMe123!" : rawPassword;
    }
}
