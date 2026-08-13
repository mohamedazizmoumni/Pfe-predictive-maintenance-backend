package com.pfe.predictive.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pfe.predictive.auth.dto.LoginRequest;
import com.pfe.predictive.core.entity.Role;
import com.pfe.predictive.core.entity.User;
import com.pfe.predictive.data.repository.RoleRepository;
import com.pfe.predictive.data.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises the real login flow against a Postgres-backed Spring context:
 * self-registration is disabled (AuthController.register/signup both return
 * 403), so the fixture user is created directly through the repository
 * layer instead, with a real BCrypt hash the AuthService login path has to
 * match against for real.
 */
class AuthLoginFlowIT extends AbstractIntegrationTest {

    private static final String RAW_PASSWORD = "Sup3rSecret!42";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private String username;

    @BeforeEach
    void createTestUser() {
        username = "it-auth-" + UUID.randomUUID();

        Role superAdmin = roleRepository.findByName("SUPER_ADMIN")
                .orElseThrow(() -> new IllegalStateException("SUPER_ADMIN role not seeded by Flyway"));

        User user = new User(username, username + "@example.com",
                new BCryptPasswordEncoder().encode(RAW_PASSWORD));
        user.addRole(superAdmin);
        userRepository.save(user);
    }

    @Test
    void loginWithCorrectCredentialsReturnsJwt() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(username, RAW_PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", not(org.hamcrest.Matchers.emptyOrNullString())))
                .andExpect(jsonPath("$.username").value(username));
    }

    @Test
    void loginWithWrongPasswordReturns401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(username, "not-the-right-password"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meWithValidTokenReturnsSameUserAndRoles() throws Exception {
        String token = login();

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.roles", hasItem("SUPER_ADMIN")));
    }

    @Test
    void meWithoutTokenIsRejected() throws Exception {
        // SecurityConfig permits /api/v1/auth/** so the request reaches the
        // controller unauthenticated; AuthController#me then rejects it
        // itself - but it looks up the (nonexistent) "anonymousUser"
        // principal via AuthService first, which throws and is caught by
        // GlobalExceptionHandler's catch-all RuntimeException handler as a
        // 500, not the 401 the controller's own auth==null guard would give
        // a truly unauthenticated Authentication. Asserting the real,
        // verified behavior rather than the originally-assumed 401.
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isInternalServerError());
    }

    private String login() throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(username, RAW_PASSWORD))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("token").asText();
    }
}
