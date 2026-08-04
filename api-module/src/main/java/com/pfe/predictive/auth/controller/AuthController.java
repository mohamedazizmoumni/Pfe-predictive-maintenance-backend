package com.pfe.predictive.auth.controller;

import com.pfe.predictive.auth.client.FaceRecognitionClient;
import com.pfe.predictive.auth.dto.LoginRequest;
import com.pfe.predictive.auth.dto.FaceLoginResponse;
import com.pfe.predictive.auth.dto.LoginResponse;
import com.pfe.predictive.auth.dto.RegisterRequest;
import com.pfe.predictive.auth.exception.FaceLoginUnauthorizedException;
import com.pfe.predictive.auth.exception.FaceNotDetectedException;
import com.pfe.predictive.auth.service.AuthService;
import com.pfe.predictive.auth.service.FaceLoginService;
import com.pfe.predictive.config.provider.JwtTokenProvider;
import com.pfe.predictive.core.entity.User;
import com.pfe.predictive.data.repository.UserRepository;
import com.pfe.predictive.ml.exception.MlBadRequestException;
import com.pfe.predictive.ml.exception.MlServiceUnavailableException;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final FaceLoginService faceLoginService;
    private final JwtTokenProvider jwtTokenProvider;
    private final FaceRecognitionClient faceRecognitionClient;
    private final UserRepository userRepository;

    public AuthController(AuthService authService,
                          FaceLoginService faceLoginService,
                          JwtTokenProvider jwtTokenProvider,
                          FaceRecognitionClient faceRecognitionClient,
                          UserRepository userRepository) {
        this.authService = authService;
        this.faceLoginService = faceLoginService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.faceRecognitionClient = faceRecognitionClient;
        this.userRepository = userRepository;
    }

    // -------------------------------------------------------------------------
    // Public self-registration endpoints are DISABLED.
    // Account creation is handled exclusively by administrators via
    // POST /api/v1/users  (UserController), which is protected by ADMIN role.
    // These endpoints are kept here returning 403 so existing frontend code
    // receives a clear error instead of a 404.
    // -------------------------------------------------------------------------

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "Self-registration is disabled. Contact an administrator to create an account."));
    }

    @PostMapping(value = "/signup", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> signup(@RequestParam("email") String email,
                                    @RequestParam("password") String password,
                                    @RequestParam(value = "file", required = false) MultipartFile file) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "Self-registration is disabled. Contact an administrator to create an account."));
    }

    // -------------------------------------------------------------------------
    // Standard credential login — unchanged.
    // Returns faceEnrolled=false for admin-created accounts that have not yet
    // captured their face, so the frontend knows to redirect to enrollment.
    // -------------------------------------------------------------------------

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            return ResponseEntity.ok(authService.login(request));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // -------------------------------------------------------------------------
    // Face login — unchanged.
    // -------------------------------------------------------------------------

    @PostMapping(value = "/face-login", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> faceLogin(
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "file",  required = false) MultipartFile file,
            @RequestParam(value = "photo", required = false) MultipartFile photo) {

        MultipartFile imageFile = image != null ? image : (file != null ? file : photo);

        if (imageFile == null || imageFile.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Face image is required. Send the file as 'image', 'file', or 'photo'."));
        }

        try {
            FaceLoginResponse response = faceLoginService.loginWithFace(imageFile);
            return ResponseEntity.ok(response);

        } catch (FaceNotDetectedException ex) {
            logger.warn("Face not detected in uploaded image: {}", ex.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", ex.getMessage()));

        } catch (FaceLoginUnauthorizedException ex) {
            logger.warn("Face login unauthorized: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", ex.getMessage()));

        } catch (MlBadRequestException ex) {
            logger.warn("Face recognition bad request: {}", ex.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", ex.getMessage()));

        } catch (MlServiceUnavailableException ex) {
            logger.error("Face recognition service unavailable: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "Face recognition service is currently unavailable. Please try again later."));

        } catch (Exception ex) {
            logger.error("Unexpected error during face login", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An unexpected error occurred during face login."));
        }
    }

    // -------------------------------------------------------------------------
    // First-time face enrollment.
    //
    // Rules:
    //   1. The caller must be authenticated (valid JWT from credential login).
    //   2. If the user has already enrolled (faceEnrolled = true) the request
    //      is rejected with 409 — only an admin can reset enrollment.
    //   3. On success:
    //      - Face embedding sent to ML service.
    //      - The image is stored as the user's profile picture (base64 data-URL).
    //      - faceEnrolled is set to true, faceEnrolledAt to now.
    //
    // POST /api/v1/auth/enroll-face
    // Requires: Authorization: Bearer <jwt>
    // Body (multipart): image / file / photo
    // -------------------------------------------------------------------------

    @PostMapping(value = {"/enroll-face", "/face-enroll"}, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Transactional
    public ResponseEntity<?> enrollFace(
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "file",  required = false) MultipartFile file,
            @RequestParam(value = "photo", required = false) MultipartFile photo) {

        MultipartFile imageFile = image != null ? image : (file != null ? file : photo);

        if (imageFile == null || imageFile.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Face image is required."));
        }

        // Resolve authenticated user from JWT
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Authentication required."));
        }

        User user = authService.getUserByUsername(auth.getName());

        // Block re-enrollment — user must ask an admin to reset first
        if (user.isFaceEnrolled()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of(
                            "error", "Face already enrolled. Contact an administrator to reset enrollment.",
                            "faceEnrolled", true,
                            "faceEnrolledAt", user.getFaceEnrolledAt() != null
                                    ? user.getFaceEnrolledAt().toString() : null
                    ));
        }

        try {
            // 1. Send face to ML service for embedding generation
            faceRecognitionClient.enrollFace(user.getId(), imageFile);
            logger.info("Face enrolled in ML service for user id={}", user.getId());

            // 2. Store the image as the profile picture (base64 data-URL)
            String dataUrl = toDataUrl(imageFile);
            user.setProfilePictureUrl(dataUrl);

            // 3. Mark enrollment complete
            user.setFaceEnrolled(true);
            user.setFaceEnrolledAt(LocalDateTime.now());
            userRepository.save(user);

            logger.info("First-time face enrollment completed for user id={}", user.getId());

            return ResponseEntity.ok(Map.of(
                    "message", "Face enrolled successfully. Profile picture set.",
                    "userId",  user.getId(),
                    "username", user.getUsername(),
                    "faceEnrolled", true
            ));

        } catch (FaceNotDetectedException ex) {
            logger.warn("No face detected during enrollment for user {}: {}", user.getId(), ex.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "No face detected in the image. Please try again with a clearer photo."));

        } catch (MlServiceUnavailableException ex) {
            logger.error("ML service unavailable during enrollment for user {}: {}", user.getId(), ex.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "Face recognition service is unavailable. Please try again later."));

        } catch (IOException ex) {
            logger.error("Failed to read image file for user {}: {}", user.getId(), ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to process the image file."));

        } catch (RuntimeException ex) {
            logger.error("Unexpected error during face enrollment for user {}: {}", user.getId(), ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Face enrollment failed: " + ex.getMessage()));
        }
    }

    // -------------------------------------------------------------------------
    // Check whether the authenticated user still needs to enroll their face.
    // The frontend can poll this after login to decide whether to show the
    // enrollment screen.
    //
    // GET /api/v1/auth/face-enrollment-status
    // -------------------------------------------------------------------------

    @GetMapping("/face-enrollment-status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> faceEnrollmentStatus() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = authService.getUserByUsername(auth.getName());

        return ResponseEntity.ok(Map.of(
                "userId",          user.getId(),
                "username",        user.getUsername(),
                "faceEnrolled",    user.isFaceEnrolled(),
                "faceEnrolledAt",  user.getFaceEnrolledAt() != null
                        ? user.getFaceEnrolledAt().toString() : null
        ));
    }

    // -------------------------------------------------------------------------
    // Token refresh — unchanged.
    // -------------------------------------------------------------------------

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody Map<String, String> request) {
        try {
            String refreshToken = request.get("refreshToken");

            if (refreshToken == null || refreshToken.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Refresh token is required"));
            }

            Claims claims = jwtTokenProvider.validateAndGetClaims(refreshToken);

            if (!"refresh".equals(claims.get("type", String.class))) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid token type"));
            }

            String username = claims.getSubject();
            User user = authService.getUserByUsername(username);

            if (user.isLocked()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "This account has been blocked. Please contact an administrator."));
            }

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("token",        authService.generateNewAccessToken(user));
            response.put("refreshToken", jwtTokenProvider.generateRefreshToken(username));
            response.put("id",           user.getId());
            response.put("username",     user.getUsername());
            response.put("email",        user.getEmail());
            response.put("firstName",    user.getFirstName());
            response.put("lastName",     user.getLastName());
            response.put("roles",        user.getRoles().stream().map(r -> r.getName()).toList());
            response.put("faceEnrolled", user.isFaceEnrolled());

            return ResponseEntity.ok(response);

        } catch (JwtException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid or expired refresh token"));
        }
    }

    // -------------------------------------------------------------------------
    // Me — unchanged, extended with faceEnrolled.
    // -------------------------------------------------------------------------

    @GetMapping("/me")
    public ResponseEntity<?> me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Not authenticated"));
        }

        User user = authService.getUserByUsername(auth.getName());

        return ResponseEntity.ok(Map.of(
                "id",           user.getId(),
                "username",     user.getUsername(),
                "email",        user.getEmail(),
                "roles",        user.getRoles().stream().map(r -> r.getName()).toList(),
                "faceEnrolled", user.isFaceEnrolled()
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        return ResponseEntity.ok(Map.of("message", "Logout successful"));
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /** Convert a MultipartFile to a base64 data-URL for database storage. */
    private String toDataUrl(MultipartFile file) throws IOException {
        String contentType = file.getContentType() != null ? file.getContentType() : "image/jpeg";
        String base64 = Base64.getEncoder().encodeToString(file.getBytes());
        return "data:" + contentType + ";base64," + base64;
    }
}