package com.pfe.predictive.auth.service;

import com.pfe.predictive.auth.client.FaceRecognitionClient;
import com.pfe.predictive.auth.dto.LoginRequest;
import com.pfe.predictive.auth.dto.LoginResponse;
import com.pfe.predictive.auth.dto.RegisterRequest;
import com.pfe.predictive.auth.exception.FaceLoginUnauthorizedException;
import com.pfe.predictive.config.provider.JwtTokenProvider;
import com.pfe.predictive.core.entity.Role;
import com.pfe.predictive.core.entity.User;
import com.pfe.predictive.data.repository.RoleRepository;
import com.pfe.predictive.data.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final FaceRecognitionClient faceRecognitionClient;
    private final BCryptPasswordEncoder passwordEncoder;
    private final PasswordEncoder delegatingPasswordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(UserRepository userRepository, 
                       RoleRepository roleRepository, 
                       FaceRecognitionClient faceRecognitionClient,
                       JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.faceRecognitionClient = faceRecognitionClient;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordEncoder = new BCryptPasswordEncoder();
        this.delegatingPasswordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    public LoginResponse signupWithFace(String email, String rawPassword, MultipartFile file) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase();
        String normalizedPassword = rawPassword == null ? "" : rawPassword.trim();

        if (normalizedEmail.isBlank() || normalizedPassword.isBlank()) {
            throw new RuntimeException("Email and password are required");
        }

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new RuntimeException("Email already exists");
        }

        String username = generateUniqueUsernameFromEmail(normalizedEmail);
        User user = new User();
        user.setUsername(username);
        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(normalizedPassword));
        user.setStatus("ACTIVE");
        user.setLocked(false);
        user.setMfaEnabled(false);

        Role technicianRole = roleRepository.findByName("TECHNICIAN")
                .orElseThrow(() -> new RuntimeException("TECHNICIAN role not found"));
        user.addRole(technicianRole);

        user = userRepository.save(user);
        logger.info("User created");

        try {
            logger.info("Sending face to Python service");
            faceRecognitionClient.enrollFace(user.getId(), file);
            logger.info("Face enrolled successfully");
        } catch (Exception ex) {
            logger.error("Face enrollment failed for userId={}: {}", user.getId(), ex.getMessage());
        }

        return createLoginResponseForUser(user);
    }

    public LoginResponse registerAndLogin(RegisterRequest request) {
        String username = request.getUsername() == null ? "" : request.getUsername().trim();
        String email = request.getEmail() == null ? "" : request.getEmail().trim().toLowerCase();
        String rawPassword = request.getPassword() == null ? "" : request.getPassword().trim();
        String firstName = request.getFirstName() == null ? "" : request.getFirstName().trim();
        String lastName = request.getLastName() == null ? "" : request.getLastName().trim();
        String department = request.getDepartment() == null ? null : request.getDepartment().trim();

        if (username.isBlank() || email.isBlank() || rawPassword.isBlank()) {
            throw new RuntimeException("Username, email and password are required");
        }

        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Username already exists");
        }

        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already exists");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setDisplayName((firstName + " " + lastName).trim());
        user.setDepartment(department);
        user.setStatus("ACTIVE");
        user.setLocked(false);
        user.setMfaEnabled(false);

        Role technicianRole = roleRepository.findByName("TECHNICIAN")
            .orElseThrow(() -> new RuntimeException("TECHNICIAN role not found"));
        user.addRole(technicianRole);

        user = userRepository.save(user);

        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getUsername(),
                user.getEmail(),
                user.getRoles().stream().map(Role::getName).collect(Collectors.toList())
        );

        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUsername());

        user.setLastLoginDate(LocalDateTime.now());
        userRepository.save(user);

        LoginResponse response = new LoginResponse();
        response.setToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setRoles(user.getRoles().stream().map(Role::getName).collect(Collectors.toList()));

        return response;
    }

    public LoginResponse login(LoginRequest request) {
        String identifier = request.getUsername() == null ? "" : request.getUsername().trim();
        String rawPassword = request.getPassword() == null ? "" : request.getPassword().trim();

        User user = resolveUserForLogin(identifier)
            .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (!isPasswordValid(user, rawPassword)) {
            throw new RuntimeException("Invalid credentials");
        }

        return createLoginResponseForUser(user);
    }

    public LoginResponse createLoginResponseForUser(User user) {
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        if (user.isLocked()) {
            throw new FaceLoginUnauthorizedException("This account has been blocked. Please contact an administrator.");
        }

        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getUsername(),
                user.getEmail(),
                user.getRoles().stream().map(Role::getName).collect(Collectors.toList())
        );

        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUsername());

        user.setLastLoginDate(LocalDateTime.now());
        userRepository.save(user);

        LoginResponse response = new LoginResponse();
        response.setToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setRoles(user.getRoles().stream().map(Role::getName).collect(Collectors.toList()));
        // Tell the frontend whether this user still needs to enroll their face
        response.setFaceEnrolled(user.isFaceEnrolled());

        return response;
    }

    private Optional<User> resolveUserForLogin(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return Optional.empty();
        }

        String normalized = identifier.trim();

        Optional<User> byUsername = userRepository.findByUsername(normalized);
        if (byUsername.isPresent()) {
            return byUsername;
        }

        Optional<User> byEmail = userRepository.findByEmail(normalized);
        if (byEmail.isPresent()) {
            return byEmail;
        }

        String lower = normalized.toLowerCase();
        return userRepository.findAll().stream()
                .filter(u -> u != null)
                .filter(u -> {
                    String uName = u.getUsername() == null ? "" : u.getUsername().trim().toLowerCase();
                    String uEmail = u.getEmail() == null ? "" : u.getEmail().trim().toLowerCase();
                    return uName.equals(lower) || uEmail.equals(lower);
                })
                .findFirst();
    }

    private boolean isPasswordValid(User user, String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            return false;
        }

        String storedPassword = user.getPassword();
        if (storedPassword == null || storedPassword.isBlank()) {
            return false;
        }

        String normalizedStoredPassword = storedPassword.trim();

        // Support Spring Security encoded formats like {bcrypt}..., {noop}..., etc.
        if (normalizedStoredPassword.startsWith("{")) {
            try {
                if (delegatingPasswordEncoder.matches(rawPassword, normalizedStoredPassword)) {
                    return true;
                }
            } catch (Exception ignored) {
                // Continue with legacy fallbacks below.
            }
        }

        if (passwordEncoder.matches(rawPassword, normalizedStoredPassword)) {
            return true;
        }

        // Dev compatibility: migrate legacy plain-text passwords to bcrypt on first valid login.
        if (rawPassword.equals(normalizedStoredPassword)) {
            user.setPassword(passwordEncoder.encode(rawPassword));
            userRepository.save(user);
            return true;
        }

        return false;
    }

    public String validateToken(String token) {
        return jwtTokenProvider.getUsernameFromToken(token);
    }

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // Used by refresh endpoint
    public String generateNewAccessToken(User user) {
        return jwtTokenProvider.generateAccessToken(
                user.getUsername(),
                user.getEmail(),
                user.getRoles().stream().map(Role::getName).collect(Collectors.toList())
        );
    }

    private String generateUniqueUsernameFromEmail(String email) {
        String base = email;
        int atIndex = email.indexOf('@');
        if (atIndex > 0) {
            base = email.substring(0, atIndex);
        }

        base = base.replaceAll("[^a-zA-Z0-9._-]", "");
        if (base.isBlank()) {
            base = "user";
        }

        String candidate = truncate(base, 50);
        if (!userRepository.existsByUsername(candidate)) {
            return candidate;
        }

        int suffix = 1;
        while (true) {
            String withSuffix = truncate(base + suffix, 50);
            if (!userRepository.existsByUsername(withSuffix)) {
                return withSuffix;
            }
            suffix++;
        }
    }

    private String truncate(String value, int maxLen) {
        if (value == null || value.length() <= maxLen) {
            return value;
        }
        return value.substring(0, maxLen);
    }
}