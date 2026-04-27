package com.pfe.predictive.user.service;

import com.pfe.predictive.user.dto.*;
import com.pfe.predictive.core.entity.User;
import com.pfe.predictive.core.entity.UserStatus;
import com.pfe.predictive.user.mapper.UserMapper;
import com.pfe.predictive.data.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * UserService - User management write operations.
 * Handles: creation, updates, password changes, role assignment, account locking/unlocking
 *
 * @author User Module
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    private static final int MAX_FAILED_LOGIN_ATTEMPTS = 5;

    public User createUser(CreateUserRequest request, String createdBy) {
        log.info("Creating new user: {}", request.getUsername());

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        String roles = request.getRoles() != null
            ? request.getRoles().stream()
                .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
                .collect(Collectors.joining(","))
            : "ROLE_TECHNICIAN";

        User user = User.builder()
            .username(request.getUsername())
            .email(request.getEmail())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .phoneNumber(request.getPhoneNumber())
            .department(request.getDepartment())
            .rolesList(roles)
            .status(UserStatus.ACTIVE)
            .failedLoginAttempts(0)
            .createdBy(createdBy)
            .build();

        User saved = userRepository.save(user);
        log.info("User created successfully: {}", saved.getId());
        return saved;
    }

    public User updateUser(Long userId, UpdateUserRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));

        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());
        if (request.getEmail() != null) {
            if (!user.getEmail().equals(request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
                throw new IllegalArgumentException("Email already in use");
            }
            user.setEmail(request.getEmail());
        }
        if (request.getPhoneNumber() != null) user.setPhoneNumber(request.getPhoneNumber());
        if (request.getDepartment() != null) user.setDepartment(request.getDepartment());

        User updated = userRepository.save(user);
        log.info("User {} updated", userId);
        return updated;
    }

    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("New passwords do not match");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setFailedLoginAttempts(0); 
        userRepository.save(user);

        log.info("Password changed for user: {}", userId);
    }

    public User assignRole(Long userId, String role) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));

        String roleWithPrefix = role.startsWith("ROLE_") ? role : "ROLE_" + role;

        if (user.getRolesAsString() != null && user.getRolesAsString().contains(roleWithPrefix)) {
            log.warn("User {} already has role {}", userId, role);
            return user;
        }

        String newRoles = user.getRolesList() == null || user.getRolesList().isEmpty()
            ? roleWithPrefix
            : user.getRolesList() + "," + roleWithPrefix;

        user.setRolesList(newRoles);
        User updated = userRepository.save(user);

        log.info("Role {} assigned to user {}", role, userId);
        return updated;
    }


    public User removeRole(Long userId, String role) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));

        String roleWithPrefix = role.startsWith("ROLE_") ? role : "ROLE_" + role;

        if (user.getRolesList() == null || !user.getRolesList().contains(roleWithPrefix)) {
            log.warn("User {} does not have role {}", userId, role);
            return user;
        }

        String newRoles = Arrays.stream(user.getRolesList().split(","))
            .filter(r -> !r.equals(roleWithPrefix))
            .collect(Collectors.joining(","));

        user.setRolesList(newRoles.isEmpty() ? "ROLE_TECHNICIAN" : newRoles);
        User updated = userRepository.save(user);

        log.info("Role {} removed from user {}", role, userId);
        return updated;
    }

    public User deactivateUser(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));

        user.setStatus(UserStatus.INACTIVE);
        User updated = userRepository.save(user);

        log.info("User {} deactivated", userId);
        return updated;
    }

    public User reactivateUser(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));

        user.setStatus(UserStatus.ACTIVE);
        user.setFailedLoginAttempts(0);
        user.setLockedDate(null);
        User updated = userRepository.save(user);

        log.info("User {} reactivated", userId);
        return updated;
    }

    public void lockAccount(Long userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setStatus(UserStatus.LOCKED);
            user.setLockedDate(LocalDateTime.now());
            userRepository.save(user);
            log.warn("User {} account locked", userId);
        });
    }

    public User unlockUser(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));

        user.setStatus(UserStatus.ACTIVE);
        user.setFailedLoginAttempts(0);
        user.setLockedDate(null);
        User updated = userRepository.save(user);

        log.info("User {} unlocked", userId);
        return updated;
    }

    public void recordSuccessfulLogin(Long userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setLastLoginDate(LocalDateTime.now());
            user.setFailedLoginAttempts(0);
            userRepository.save(user);
        });
    }


    public void recordFailedLoginAttempt(Long userId) {
        userRepository.findById(userId).ifPresent(user -> {
            int newAttempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(newAttempts);

            if (newAttempts >= MAX_FAILED_LOGIN_ATTEMPTS) {
                user.setStatus(UserStatus.LOCKED);
                user.setLockedDate(LocalDateTime.now());
                log.warn("User {} locked after {} failed attempts", userId, newAttempts);
            }

            userRepository.save(user);
        });
    }

    public void deleteUser(Long userId) {
        log.warn("Deleting user: {}", userId);
        userRepository.deleteById(userId);
    }


    public User getUserById(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));
    }

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new EntityNotFoundException("User not found with username: " + username));
    }
}
