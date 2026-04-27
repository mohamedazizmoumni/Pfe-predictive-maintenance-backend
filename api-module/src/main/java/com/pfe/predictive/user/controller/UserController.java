package com.pfe.predictive.user.controller;

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
import org.springframework.security.crypto.password.PasswordEncoder;
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
import java.util.Set;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserController(UserRepository userRepository,
                          RoleRepository roleRepository,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

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

    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        return userRepository.findById(id)
                .map(user -> ResponseEntity.ok(toDto(user)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
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

            return ResponseEntity.status(isNewUser ? 201 : 200).body(toDto(userRepository.save(user)));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("User not found"));

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

            if (request.getFirstName() != null) {
                user.setFirstName(request.getFirstName());
            }
            if (request.getLastName() != null) {
                user.setLastName(request.getLastName());
            }
            if (request.getPhoneNumber() != null) {
                user.setPhoneNumber(request.getPhoneNumber());
            }
            if (request.getDepartment() != null) {
                user.setDepartment(request.getDepartment());
            }
            if (request.getStatus() != null && !request.getStatus().isBlank()) {
                user.setStatus(request.getStatus());
                if ("LOCKED".equalsIgnoreCase(request.getStatus())) {
                    user.setLocked(true);
                } else if ("ACTIVE".equalsIgnoreCase(request.getStatus()) || "INACTIVE".equalsIgnoreCase(request.getStatus())) {
                    user.setLocked(false);
                }
            }
            if (request.getRoles() != null) {
                user.setRoles(resolveRoles(request.getRoles()));
            }

            return ResponseEntity.ok(toDto(userRepository.save(user)));
        } catch (EntityNotFoundException ex) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        if (!userRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        userRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

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
            if (roleName == null || roleName.isBlank()) {
                continue;
            }

            String normalized = roleName.trim().toUpperCase();
            if (normalized.startsWith("ROLE_")) {
                normalized = normalized.substring(5);
            }

            addRoleByName(resolvedRoles, normalized);
        }

        if (resolvedRoles.isEmpty()) {
            addRoleByName(resolvedRoles, "TECHNICIAN");
        }

        return resolvedRoles;
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
        if (rawPassword == null || rawPassword.isBlank()) {
            return "ChangeMe123!";
        }
        return rawPassword;
    }
}
