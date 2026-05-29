package com.pfe.predictive.user.controller;

import com.pfe.predictive.user.dto.ProfilePictureResponse;
import com.pfe.predictive.user.dto.ProfilePictureDeleteResponse;
import com.pfe.predictive.user.service.ProfileService;
import com.pfe.predictive.data.repository.UserRepository;
import com.pfe.predictive.core.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * UserProfilePictureController - Alternative endpoints for profile picture management
 * Provides endpoints at /api/v1/users/{username}/profile-picture for frontend compatibility
 *
 * @author User Module
 * @version 1.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserProfilePictureController {

    private final ProfileService profileService;
    private final UserRepository userRepository;

    // ============================================================================
    // PROFILE PICTURE MANAGEMENT - BY USERNAME
    // ============================================================================

    /**
     * Upload profile picture by username
     * POST /api/v1/users/{username}/profile-picture
     */
    @PostMapping(value = "/{username}/profile-picture", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProfilePictureResponse> uploadProfilePictureByUsername(
            @PathVariable String username,
            @RequestParam("file") MultipartFile file) {
        log.info("Uploading profile picture for user: {}", username);

        try {
            // Find user by username
            User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + username));

            String pictureUrl = profileService.uploadProfilePicture(user.getId(), file);

            ProfilePictureResponse response = ProfilePictureResponse.builder()
                .userId(user.getId())
                .profilePictureUrl(pictureUrl)
                .message("Profile picture uploaded successfully")
                .uploadedAt(LocalDateTime.now())
                .build();

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            log.warn("Profile picture validation error for user {}: {}", username, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (EntityNotFoundException e) {
            log.warn("User not found: {}", username);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error uploading profile picture for user {}: {}", username, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get profile picture URL by username
     * GET /api/v1/users/{username}/profile-picture
     */
    @GetMapping("/{username}/profile-picture")
    public ResponseEntity<?> getProfilePictureByUsername(@PathVariable String username) {
        log.info("Fetching profile picture for user: {}", username);

        try {
            // Find user by username
            User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + username));

            String pictureUrl = profileService.getProfilePictureUrl(user.getId());

            if (pictureUrl == null || pictureUrl.isBlank()) {
                return ResponseEntity.noContent().build();
            }

            return ResponseEntity.ok(Map.of("profilePictureUrl", pictureUrl));

        } catch (EntityNotFoundException e) {
            log.warn("User not found: {}", username);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error fetching profile picture for user {}: {}", username, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Delete profile picture by username
     * DELETE /api/v1/users/{username}/profile-picture
     */
    @DeleteMapping("/{username}/profile-picture")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProfilePictureDeleteResponse> deleteProfilePictureByUsername(
            @PathVariable String username) {
        log.info("Deleting profile picture for user: {}", username);

        try {
            // Find user by username
            User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + username));

            profileService.deleteProfilePicture(user.getId());

            ProfilePictureDeleteResponse response = ProfilePictureDeleteResponse.builder()
                .userId(user.getId())
                .message("Profile picture deleted successfully")
                .deletedAt(LocalDateTime.now())
                .build();

            return ResponseEntity.ok(response);

        } catch (EntityNotFoundException e) {
            log.warn("User not found: {}", username);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error deleting profile picture for user {}: {}", username, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Check if user has profile picture by username
     * GET /api/v1/users/{username}/profile-picture/exists
     */
    @GetMapping("/{username}/profile-picture/exists")
    public ResponseEntity<Map<String, Boolean>> hasProfilePictureByUsername(@PathVariable String username) {
        log.info("Checking if user {} has profile picture", username);

        try {
            // Find user by username
            User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + username));

            boolean hasImage = profileService.hasProfilePicture(user.getId());
            return ResponseEntity.ok(Map.of("hasProfilePicture", hasImage));

        } catch (EntityNotFoundException e) {
            log.warn("User not found: {}", username);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error checking profile picture for user {}: {}", username, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
