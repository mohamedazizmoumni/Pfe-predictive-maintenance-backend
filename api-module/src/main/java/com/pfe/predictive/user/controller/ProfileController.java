package com.pfe.predictive.user.controller;

import com.pfe.predictive.user.dto.*;
import com.pfe.predictive.user.service.ProfileService;
import com.pfe.predictive.data.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * ProfileController - User profile management endpoints
 * Handles profile retrieval, updates, and picture management
 *
 * @author User Module
 * @version 1.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;
    private final com.pfe.predictive.data.repository.UserRepository userRepository;

    // ============================================================================
    // PROFILE RETRIEVAL
    // ============================================================================

    /**
     * Get current user's profile
     * GET /api/v1/profile
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProfileResponse> getCurrentProfile() {
        Long userId = getCurrentUserId();
        log.info("Fetching profile for user: {}", userId);

        ProfileResponse profile = profileService.getProfile(userId);
        return ResponseEntity.ok(profile);
    }

    /**
     * Get user profile by ID (admin only)
     * GET /api/v1/profile/{userId}
     */
    @GetMapping("/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or #userId == authentication.principal.id")
    public ResponseEntity<ProfileResponse> getUserProfile(@PathVariable Long userId) {
        log.info("Fetching profile for user: {}", userId);

        ProfileResponse profile = profileService.getProfile(userId);
        return ResponseEntity.ok(profile);
    }

    // ============================================================================
    // PROFILE UPDATE
    // ============================================================================

    /**
     * Update current user's profile
     * PUT /api/v1/profile
     */
    @PutMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProfileResponse> updateCurrentProfile(
            @Valid @RequestBody UpdateProfileRequest request) {
        Long userId = getCurrentUserId();
        log.info("Updating profile for user: {}", userId);

        try {
            ProfileResponse updated = profileService.updateProfile(userId, request);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            log.warn("Profile update validation error for user {}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Update user profile by ID (admin only)
     * PUT /api/v1/profile/{userId}
     */
    @PutMapping("/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or #userId == authentication.principal.id")
    public ResponseEntity<ProfileResponse> updateUserProfile(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateProfileRequest request) {
        log.info("Updating profile for user: {}", userId);

        try {
            ProfileResponse updated = profileService.updateProfile(userId, request);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            log.warn("Profile update validation error for user {}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest().body(null);
        }
    }

    // ============================================================================
    // PROFILE PICTURE MANAGEMENT
    // ============================================================================

    /**
     * Upload profile picture for current user
     * POST /api/v1/profile/picture
     * Accepts field name "file", "image", or "photo" for flexibility.
     */
    @PostMapping(value = "/picture", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProfilePictureResponse> uploadProfilePicture(
            @RequestParam(value = "file",  required = false) MultipartFile file,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "photo", required = false) MultipartFile photo) {
        Long userId = getCurrentUserId();
        log.info("Uploading profile picture for user: {}", userId);

        MultipartFile upload = file != null ? file : (image != null ? image : photo);

        try {
            String pictureUrl = profileService.uploadProfilePicture(userId, upload);

            ProfilePictureResponse response = ProfilePictureResponse.builder()
                .userId(userId)
                .profilePictureUrl(pictureUrl)
                .message("Profile picture uploaded successfully")
                .uploadedAt(LocalDateTime.now())
                .build();

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            log.warn("Profile picture validation error for user {}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest().body(
                ProfilePictureResponse.builder()
                    .userId(userId)
                    .message(e.getMessage())
                    .build());
        } catch (Exception e) {
            log.error("Error uploading profile picture for user {}: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ProfilePictureResponse.builder()
                    .userId(userId)
                    .message("Failed to upload profile picture: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Upload profile picture for user by ID (admin only)
     * POST /api/v1/profile/{userId}/picture
     */
    @PostMapping(value = "/{userId}/picture", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or #userId == authentication.principal.id")
    public ResponseEntity<ProfilePictureResponse> uploadUserProfilePicture(
            @PathVariable Long userId,
            @RequestParam(value = "file",  required = false) MultipartFile file,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "photo", required = false) MultipartFile photo) {
        log.info("Uploading profile picture for user: {}", userId);

        MultipartFile upload = file != null ? file : (image != null ? image : photo);

        try {
            String pictureUrl = profileService.uploadProfilePicture(userId, upload);

            ProfilePictureResponse response = ProfilePictureResponse.builder()
                .userId(userId)
                .profilePictureUrl(pictureUrl)
                .message("Profile picture uploaded successfully")
                .uploadedAt(LocalDateTime.now())
                .build();

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            log.warn("Profile picture validation error for user {}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest().body(
                ProfilePictureResponse.builder()
                    .userId(userId)
                    .message(e.getMessage())
                    .build());
        } catch (Exception e) {
            log.error("Error uploading profile picture for user {}: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ProfilePictureResponse.builder()
                    .userId(userId)
                    .message("Failed to upload profile picture: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Get profile picture URL for current user
     * GET /api/v1/profile/picture
     */
    @GetMapping("/picture")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getProfilePicture() {
        Long userId = getCurrentUserId();
        log.info("Fetching profile picture for user: {}", userId);

        try {
            String pictureUrl = profileService.getProfilePictureUrl(userId);

            if (pictureUrl == null || pictureUrl.isBlank()) {
                return ResponseEntity.noContent().build();
            }

            return ResponseEntity.ok(Map.of("profilePictureUrl", pictureUrl));

        } catch (Exception e) {
            log.error("Error fetching profile picture for user {}: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get profile picture URL for user by ID
     * GET /api/v1/profile/{userId}/picture
     */
    @GetMapping("/{userId}/picture")
    public ResponseEntity<?> getUserProfilePicture(@PathVariable Long userId) {
        log.info("Fetching profile picture for user: {}", userId);

        try {
            String pictureUrl = profileService.getProfilePictureUrl(userId);

            if (pictureUrl == null || pictureUrl.isBlank()) {
                return ResponseEntity.noContent().build();
            }

            return ResponseEntity.ok(Map.of("profilePictureUrl", pictureUrl));

        } catch (Exception e) {
            log.error("Error fetching profile picture for user {}: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Delete profile picture for current user
     * DELETE /api/v1/profile/picture
     */
    @DeleteMapping("/picture")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProfilePictureDeleteResponse> deleteProfilePicture() {
        Long userId = getCurrentUserId();
        log.info("Deleting profile picture for user: {}", userId);

        try {
            profileService.deleteProfilePicture(userId);

            ProfilePictureDeleteResponse response = ProfilePictureDeleteResponse.builder()
                .userId(userId)
                .message("Profile picture deleted successfully")
                .deletedAt(LocalDateTime.now())
                .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error deleting profile picture for user {}: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Delete profile picture for user by ID (admin only)
     * DELETE /api/v1/profile/{userId}/picture
     */
    @DeleteMapping("/{userId}/picture")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or #userId == authentication.principal.id")
    public ResponseEntity<ProfilePictureDeleteResponse> deleteUserProfilePicture(
            @PathVariable Long userId) {
        log.info("Deleting profile picture for user: {}", userId);

        try {
            profileService.deleteProfilePicture(userId);

            ProfilePictureDeleteResponse response = ProfilePictureDeleteResponse.builder()
                .userId(userId)
                .message("Profile picture deleted successfully")
                .deletedAt(LocalDateTime.now())
                .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error deleting profile picture for user {}: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Check if user has profile picture
     * GET /api/v1/profile/{userId}/picture/exists
     */
    @GetMapping("/{userId}/picture/exists")
    public ResponseEntity<Map<String, Boolean>> hasProfilePicture(@PathVariable Long userId) {
        log.info("Checking if user {} has profile picture", userId);

        boolean hasImage = profileService.hasProfilePicture(userId);
        return ResponseEntity.ok(Map.of("hasProfilePicture", hasImage));
    }

    // ============================================================================
    // HELPER METHODS
    // ============================================================================

    /**
     * Get current authenticated user ID.
     * The JWT filter sets the Authentication principal to the plain username
     * String (not a UserDetails), so resolve via Authentication#getName().
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new IllegalStateException("User not authenticated");
        }
        String username = authentication.getName();
        return userRepository.findByUsername(username)
            .map(com.pfe.predictive.core.entity.User::getId)
            .orElseThrow(() -> new IllegalStateException("User not found: " + username));
    }
}
