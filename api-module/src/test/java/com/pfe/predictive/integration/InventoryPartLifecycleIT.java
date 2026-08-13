package com.pfe.predictive.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pfe.predictive.auth.dto.LoginRequest;
import com.pfe.predictive.core.entity.Role;
import com.pfe.predictive.core.entity.User;
import com.pfe.predictive.data.repository.RoleRepository;
import com.pfe.predictive.data.repository.UserRepository;
import com.pfe.predictive.inventory.dto.PartRequest;
import com.pfe.predictive.inventory.dto.PartUpdateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Real create -> fetch -> update flow for parts through the actual HTTP
 * layer (MockMvc: real DispatcherServlet + Spring Security filter chain),
 * backed by Postgres. InventoryController requires
 * STOCK_MANAGER/MANAGER/ADMIN/SUPER_ADMIN for writes (@PreAuthorize on the
 * controller), so the fixture user is granted SUPER_ADMIN, same as the
 * other IT classes.
 */
class InventoryPartLifecycleIT extends AbstractIntegrationTest {

    private static final String RAW_PASSWORD = "Sup3rSecret!42";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private String token;

    @BeforeEach
    void createAuthenticatedUser() throws Exception {
        String username = "it-inv-" + UUID.randomUUID();

        Role superAdmin = roleRepository.findByName("SUPER_ADMIN")
                .orElseThrow(() -> new IllegalStateException("SUPER_ADMIN role not seeded by Flyway"));

        User user = new User(username, username + "@example.com",
                new BCryptPasswordEncoder().encode(RAW_PASSWORD));
        user.addRole(superAdmin);
        userRepository.save(user);

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(username, RAW_PASSWORD))))
                .andExpect(status().isOk())
                .andReturn();
        token = objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("token").asText();
    }

    @Test
    void createFetchAndUpdatePartPersistsToPostgres() throws Exception {
        String partNumber = "IT-PN-" + UUID.randomUUID();

        PartRequest createRequest = PartRequest.builder()
                .name("Integration Test Bearing")
                .partNumber(partNumber)
                .currentStock(20)
                .minimumStock(5)
                .cost(new BigDecimal("15.50"))
                .unit("pcs")
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/inventory/parts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.partNumber").value(partNumber))
                .andExpect(jsonPath("$.status").value("AVAILABLE"))
                .andReturn();

        long partId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/v1/inventory/parts/" + partId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStock").value(20))
                .andExpect(jsonPath("$.partNumber").value(partNumber));

        PartUpdateRequest updateRequest = PartUpdateRequest.builder()
                .currentStock(3)
                .build();

        mockMvc.perform(put("/api/v1/inventory/parts/" + partId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStock").value(3))
                .andExpect(jsonPath("$.status").value("LOW_STOCK"));

        MvcResult refetchResult = mockMvc.perform(get("/api/v1/inventory/parts/" + partId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        var refetched = objectMapper.readTree(refetchResult.getResponse().getContentAsString());
        assertThat(refetched.get("currentStock").asInt()).isEqualTo(3);
        assertThat(refetched.get("status").asText()).isEqualTo("LOW_STOCK");
    }

    @Test
    void createPartRejectsDuplicatePartNumberThroughRealHttp() throws Exception {
        String partNumber = "IT-PN-DUP-" + UUID.randomUUID();

        PartRequest request = PartRequest.builder()
                .name("First part")
                .partNumber(partNumber)
                .currentStock(10)
                .minimumStock(2)
                .build();

        mockMvc.perform(post("/api/v1/inventory/parts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        PartRequest duplicateRequest = PartRequest.builder()
                .name("Second part")
                .partNumber(partNumber)
                .currentStock(1)
                .minimumStock(1)
                .build();

        mockMvc.perform(post("/api/v1/inventory/parts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isBadRequest());
    }
}
