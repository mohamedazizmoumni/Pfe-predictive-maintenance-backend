package com.pfe.predictive.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pfe.predictive.auth.dto.LoginRequest;
import com.pfe.predictive.core.entity.Role;
import com.pfe.predictive.core.entity.User;
import com.pfe.predictive.data.repository.RoleRepository;
import com.pfe.predictive.data.repository.UserRepository;
import com.pfe.predictive.task.TaskRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Real create -> update-status -> fetch flow for tasks through the actual
 * HTTP layer (MockMvc: real DispatcherServlet + Spring Security filter
 * chain), backed by Postgres. TaskController requires MANAGER/ADMIN/
 * SUPER_ADMIN to create and TECHNICIAN/MANAGER/ADMIN/SUPER_ADMIN to update
 * (@PreAuthorize on the controller), so the fixture user is granted
 * SUPER_ADMIN, same as the other IT classes. No technician is assigned, so
 * TaskService never attempts to send an assignment email (see
 * TaskServiceTest's createTaskSkipsEmailWhenNoTechnicianAssigned for the
 * same rule).
 */
class TaskLifecycleIT extends AbstractIntegrationTest {

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
        String username = "it-task-" + UUID.randomUUID();

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
    void createUpdateStatusAndFetchTaskPersistsToPostgres() throws Exception {
        TaskRequest createRequest = TaskRequest.builder()
                .title("Replace conveyor belt")
                .description("Belt showing wear on section 3")
                .priority("HIGH")
                .dueDate(LocalDateTime.now().plusDays(2))
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andReturn();

        long taskId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        // TaskRequest is reused for both create and update, and its bean
        // validation (@NotBlank title, @NotNull dueDate) is enforced by
        // @Valid on both controller methods - an update payload carrying
        // only the changed field would 400. Values that differ from the
        // create request just verify the update actually overwrites them.
        TaskRequest transitionRequest = TaskRequest.builder()
                .title("Replace conveyor belt")
                .status("IN_PROGRESS")
                .dueDate(LocalDateTime.now().plusDays(3))
                .build();

        mockMvc.perform(put("/api/v1/tasks/" + taskId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transitionRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.title").value("Replace conveyor belt"))
                .andExpect(jsonPath("$.priority").value("HIGH"));

        mockMvc.perform(get("/api/v1/tasks/" + taskId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.title").value("Replace conveyor belt"));
    }

    @Test
    void createTaskRejectsUnknownMachineThroughRealHttp() throws Exception {
        TaskRequest request = TaskRequest.builder()
                .title("Task against a machine that doesn't exist")
                .machineId(999_999L)
                .dueDate(LocalDateTime.now().plusDays(1))
                .build();

        mockMvc.perform(post("/api/v1/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
