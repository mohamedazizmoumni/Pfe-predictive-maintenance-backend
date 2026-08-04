package com.pfe.predictive.user.service;

import com.pfe.predictive.auth.client.FaceRecognitionClient;
import com.pfe.predictive.auth.exception.FaceNotDetectedException;
import com.pfe.predictive.core.entity.User;
import com.pfe.predictive.data.repository.UserRepository;
import com.pfe.predictive.user.dto.ProfileResponse;
import com.pfe.predictive.user.dto.UpdateProfileRequest;
import com.pfe.predictive.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import jakarta.persistence.EntityNotFoundException;
import java.io.IOException;
import java.util.Base64;

/**
 * ProfileService - User profile management operations
 * Handles: profile retrieval, updates, and picture management
 *
 * @author User Module
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ProfileService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final FaceRecognitionClient faceRecognitionClient;

    @Value("${app.profile.picture.max-size:5242880}") // 5MB default
    private long maxFileSize;

    @Value("${app.profile.picture.storage-path:uploads/profiles}")
    private String storagePath;

    @Value("${app.profile.picture.base-url:http://localhost:8080/api/v1/users}")
    private String baseUrl;

    private static final String[] ALLOWED_MIME_TYPES = {
        "image/jpeg", "image/png", "image/gif", "image/webp", "image/jpg"
    };

    // ============================================================================
    // PROFILE RETRIEVAL
    // ============================================================================

    /**
     * Get user profile by ID
     */
    @Transactional(readOnly = true)
    public ProfileResponse getProfile(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));

        return userMapper.toProfileResponse(user);
    }

    /**
     * Get current user profile (for authenticated user)
     */
    @Transactional(readOnly = true)
    public ProfileResponse getCurrentUserProfile(Long userId) {
        return getProfile(userId);
    }

    // ============================================================================
    // PROFILE UPDATE
    // ============================================================================

    /**
     * Update user profile information
     * Does not update username or password
     */
    public ProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));

        // Update email if provided and not already in use
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            if (!user.getEmail().equalsIgnoreCase(request.getEmail())
                    && userRepository.existsByEmail(request.getEmail())) {
                throw new IllegalArgumentException("Email already in use");
            }
            user.setEmail(request.getEmail());
        }

        // Update other profile fields
        if (request.getFirstName() != null && !request.getFirstName().isBlank()) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null && !request.getLastName().isBlank()) {
            user.setLastName(request.getLastName());
        }
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getDepartment() != null) {
            user.setDepartment(request.getDepartment());
        }
        if (request.getDisplayName() != null) {
            user.setDisplayName(request.getDisplayName());
        }

        User updated = userRepository.save(user);
        log.info("Profile updated for user: {}", userId);

        return userMapper.toProfileResponse(updated);
    }

    // ============================================================================
    // PROFILE PICTURE MANAGEMENT
    // ============================================================================

    /**
     * Upload profile picture for user.
     * Validates file type and size, stores as base64 in DB, and additionally
     * enrolls the image as a supplementary face embedding in the ML service.
     * If the image contains no detectable face the picture is still saved —
     * a non-fatal warning is logged so the user experience is unaffected.
     */
    public String uploadProfilePicture(Long userId, MultipartFile file) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));

        // Validate file
        validateProfilePicture(file);

        try {
            // Convert to base64 data-URL and persist to database
            String base64Picture = Base64.getEncoder().encodeToString(file.getBytes());
            String dataUrl = "data:" + file.getContentType() + ";base64," + base64Picture;

            user.setProfilePictureUrl(dataUrl);
            userRepository.save(user);

            log.info("Profile picture saved for user {}", userId);

            // Enroll the new image as an additional face reference in the ML service.
            // This runs *after* the DB save so a face-detection failure never blocks
            // the profile-picture upload itself.
            tryEnrollFaceFromProfilePicture(userId, file);

            return dataUrl;

        } catch (IOException e) {
            log.error("Error reading file for user {}: {}", userId, e.getMessage());
            throw new RuntimeException("Failed to process image file", e);
        }
    }

    /**
     * Attempts to enroll the uploaded profile picture as an additional face embedding.
     * Errors are swallowed so that a non-face image (avatar, logo, etc.) never
     * prevents the profile-picture upload from completing.
     */
    private void tryEnrollFaceFromProfilePicture(Long userId, MultipartFile file) {
        try {
            faceRecognitionClient.enrollFace(userId, file);
            log.info("Profile picture also enrolled as face reference for user {}", userId);
        } catch (FaceNotDetectedException ex) {
            // Profile photo contains no recognisable face — perfectly fine.
            log.warn("No face detected in profile picture for user {} – face reference not updated. {}",
                    userId, ex.getMessage());
        } catch (Exception ex) {
            // ML service unavailable, network timeout, etc. — do not fail the upload.
            log.warn("Could not enroll profile picture as face reference for user {} (non-fatal): {}",
                    userId, ex.getMessage());
        }
    }

    /**
     * Get profile picture URL for user
     */
    @Transactional(readOnly = true)
    public String getProfilePictureUrl(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));

        return user.getProfilePictureUrl();
    }

    /**
     * Delete profile picture for user
     */
    public void deleteProfilePicture(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));

        user.setProfilePictureUrl(null);
        userRepository.save(user);

        log.info("Profile picture deleted for user: {}", userId);
    }

    /**
     * Check if user has profile picture
     */
    @Transactional(readOnly = true)
    public boolean hasProfilePicture(Long userId) {
        return userRepository.findById(userId)
            .map(user -> user.getProfilePictureUrl() != null && !user.getProfilePictureUrl().isBlank())
            .orElse(false);
    }

    // ============================================================================
    // VALIDATION
    // ============================================================================

    /**
     * Validate profile picture file
     */
    private void validateProfilePicture(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required");
        }

        // Check file size
        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException(
                String.format("File size exceeds maximum allowed size of %d MB",
                    maxFileSize / (1024 * 1024))
            );
        }

        // Check MIME type
        String contentType = file.getContentType();
        if (contentType == null || !isAllowedMimeType(contentType)) {
            throw new IllegalArgumentException(
                "Invalid file type. Allowed types: JPEG, PNG, GIF, WebP"
            );
        }

        // Check file extension
        String filename = file.getOriginalFilename();
        if (filename == null || !isAllowedExtension(filename)) {
            throw new IllegalArgumentException(
                "Invalid file extension. Allowed: jpg, jpeg, png, gif, webp"
            );
        }
    }

    /**
     * Check if MIME type is allowed
     */
    private boolean isAllowedMimeType(String contentType) {
        for (String allowed : ALLOWED_MIME_TYPES) {
            if (contentType.equalsIgnoreCase(allowed)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if file extension is allowed
     */
    private boolean isAllowedExtension(String filename) {
        String[] allowedExtensions = {"jpg", "jpeg", "png", "gif", "webp"};
        String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();

        for (String allowed : allowedExtensions) {
            if (extension.equals(allowed)) {
                return true;
            }
        }
        return false;
    }
}
