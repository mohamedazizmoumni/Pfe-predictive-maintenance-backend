package com.pfe.predictive.auth.controller;

import com.pfe.predictive.auth.dto.LoginRequest;
import com.pfe.predictive.auth.dto.FaceLoginResponse;
import com.pfe.predictive.auth.dto.LoginResponse;
import com.pfe.predictive.auth.dto.RegisterRequest;
import com.pfe.predictive.auth.service.AuthService;
import com.pfe.predictive.auth.service.FaceLoginService;
import com.pfe.predictive.config.provider.JwtTokenProvider;
import com.pfe.predictive.core.entity.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final FaceLoginService faceLoginService;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthController(AuthService authService,
                          FaceLoginService faceLoginService,
                          JwtTokenProvider jwtTokenProvider) {
        this.authService = authService;
        this.faceLoginService = faceLoginService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(authService.registerAndLogin(request));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping(value = "/signup", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> signup(@RequestParam("email") String email,
                                    @RequestParam("password") String password,
                                    @RequestParam(value = "file", required = false) MultipartFile file) {

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Face image file is required"));
        }

        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(authService.signupWithFace(email, password, file));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            return ResponseEntity.ok(authService.login(request));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping(value = "/face-login", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FaceLoginResponse> faceLogin(@RequestParam("image") MultipartFile image) {
        return ResponseEntity.ok(faceLoginService.loginWithFace(image));
    }

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

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("token", authService.generateNewAccessToken(user));
            response.put("refreshToken", jwtTokenProvider.generateRefreshToken(username));
            response.put("id", user.getId());
            response.put("username", user.getUsername());
            response.put("email", user.getEmail());
            response.put("firstName", user.getFirstName());
            response.put("lastName", user.getLastName());
            response.put("roles", user.getRoles().stream().map(r -> r.getName()).toList());

            return ResponseEntity.ok(response);

        } catch (JwtException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid or expired refresh token"));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Not authenticated"));
        }

        User user = authService.getUserByUsername(auth.getName());

        return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "email", user.getEmail(),
                "roles", user.getRoles().stream().map(r -> r.getName()).toList()
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        return ResponseEntity.ok(Map.of("message", "Logout successful"));
    }
}