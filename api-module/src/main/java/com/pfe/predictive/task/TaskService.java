package com.pfe.predictive.task;

import com.pfe.predictive.common.service.AssignmentEmailContent;
import com.pfe.predictive.common.service.EmailService;
import com.pfe.predictive.core.entity.Task;
import com.pfe.predictive.core.entity.User;
import com.pfe.predictive.data.repository.MachineRepository;
import com.pfe.predictive.data.repository.MaintenanceRepository;
import com.pfe.predictive.data.repository.TaskRepository;
import com.pfe.predictive.data.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TaskService {

    private static final DateTimeFormatter EMAIL_DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a");

    private static final String DEFAULT_FRONTEND_URL = "http://localhost:4200";

    // These list endpoints have no client-driven paging yet - cap at a
    // generous size instead of loading every row ever created.
    private static final int LIST_CAP = 200;

    private final TaskRepository taskRepository;
    private final EmailService emailService;
    private final UserRepository userRepository;
    private final MachineRepository machineRepository;
    private final MaintenanceRepository maintenanceRepository;

    @Value("${app.frontend-url:" + DEFAULT_FRONTEND_URL + "}")
    private String frontendUrl;

    /**
     * Create new task
     */
    public Task createTask(TaskRequest request) {

        log.info("==================================================");
        log.info("CREATING NEW TASK");
        log.info("==================================================");

        log.info("Title: {}", request.getTitle());
        log.info(
                "AssignedTechnicianId: {}",
                request.getAssignedTechnicianId()
        );
        log.info("MachineId: {}", request.getMachineId());

        validateTaskReferences(request);

        Task task = new Task();

        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setMachineId(request.getMachineId());
        task.setMaintenanceId(request.getMaintenanceId());

        // =====================================================
        // ASSIGNED TECHNICIAN
        // =====================================================

        if (request.getAssignedTechnicianId() != null) {

            log.info(
                    "Assigned technician ID received: {}",
                    request.getAssignedTechnicianId()
            );

            task.setAssignedTo(
                    String.valueOf(request.getAssignedTechnicianId())
            );

        } else {

            log.warn("NO assignedTechnicianId RECEIVED FROM FRONTEND");
        }

        task.setPriority(
                request.getPriority() != null
                        ? request.getPriority()
                        : "MEDIUM"
        );

        task.setStatus(
                request.getStatus() != null
                        ? request.getStatus()
                        : "PENDING"
        );

        task.setDueDate(request.getDueDate());

        task.setCreatedAt(LocalDateTime.now());

        Task saved = taskRepository.save(task);
        String assignedBy = resolveCurrentAssignerDisplayName();

        log.info("TASK CREATED SUCCESSFULLY");
        log.info("Task ID: {}", saved.getId());

        // =========================================================================
        // SEND EMAIL TO ASSIGNED TECHNICIAN
        // =========================================================================

        if (saved.getAssignedTo() == null || saved.getAssignedTo().isBlank()) {

            log.warn("NO TECHNICIAN ASSIGNED");
            return saved;
        }

        sendAssignmentEmail(saved, assignedBy);

        return saved;
    }

    /**
     * Get task by ID
     */
    @Transactional(readOnly = true)
    public Task getTaskById(Long id) {

        log.debug("Fetching task: {}", id);

        return taskRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Task not found: " + id
                        )
                );
    }

    /**
     * Get all tasks
     */
    @Transactional(readOnly = true)
    public List<Task> getAllTasks() {

        log.debug("Fetching all tasks");

        return taskRepository.findAll(PageRequest.of(0, LIST_CAP)).getContent();
    }

    /**
     * Update task
     */
    public Task updateTask(Long id, TaskRequest request) {

        log.info("Updating task: {}", id);

        Task task = getTaskById(id);
        String previousAssignedTo = task.getAssignedTo();

        if (request.getTitle() != null) {
            task.setTitle(request.getTitle());
        }

        if (request.getDescription() != null) {
            task.setDescription(request.getDescription());
        }

        if (request.getMachineId() != null) {
            task.setMachineId(request.getMachineId());
        }

        if (request.getMaintenanceId() != null) {
            task.setMaintenanceId(request.getMaintenanceId());
        }

        // =====================================================
        // UPDATE ASSIGNED TECHNICIAN
        // =====================================================

        if (request.getAssignedTechnicianId() != null) {

            task.setAssignedTo(
                    String.valueOf(request.getAssignedTechnicianId())
            );
        }

        if (request.getPriority() != null) {
            task.setPriority(request.getPriority());
        }

        if (request.getStatus() != null) {
            task.setStatus(request.getStatus());
        }

        if (request.getDueDate() != null) {
            task.setDueDate(request.getDueDate());
        }

        task.setUpdatedAt(LocalDateTime.now());

        Task updated = taskRepository.save(task);

        if (request.getAssignedTechnicianId() != null
                && !Objects.equals(previousAssignedTo, updated.getAssignedTo())) {

            sendAssignmentEmail(updated, resolveCurrentAssignerDisplayName());
        }

        log.info("Task updated successfully: {}", id);

        return updated;
    }

    private void sendAssignmentEmail(Task task, String assignedBy) {
        try {
            if (task.getAssignedTo() == null || task.getAssignedTo().isBlank()) {
                log.warn("Skipping task assignment email: no technician assigned");
                return;
            }

            Long technicianId;
            try {
                technicianId = Long.parseLong(task.getAssignedTo());
            } catch (NumberFormatException ex) {
                log.error("assignedTo is not a valid numeric id: {}", task.getAssignedTo());
                return;
            }

            User technician = userRepository.findById(technicianId).orElse(null);
            if (technician == null || technician.getEmail() == null || technician.getEmail().isBlank()) {
                log.warn("Skipping task assignment email #{} — technician not found or has no email", task.getId());
                return;
            }

            AssignmentEmailContent content = new AssignmentEmailContent(
                    resolveRecipientName(technician),
                    "Task",
                    task.getId(),
                    task.getTitle(),
                    task.getDescription(),
                    task.getPriority(),
                    task.getStatus(),
                    task.getMachineId(),
                    "Due Date",
                    formatDateTime(task.getDueDate()),
                    assignedBy,
                    buildTaskUrl(task.getId()),
                    "Open Task"
            );

            emailService.sendAssignmentNotification(technician.getEmail(), content);
        } catch (Exception ex) {
            log.error("Failed to send task assignment email for task #{}: {}", task.getId(), ex.getMessage(), ex);
        }
    }

    private String buildTaskUrl(Long taskId) {
        String baseUrl = normalizeFrontendUrl(frontendUrl);
        return baseUrl + "/tasks/" + taskId;
    }

    private String normalizeFrontendUrl(String value) {
        String normalized = value == null || value.isBlank() ? DEFAULT_FRONTEND_URL : value.trim();

        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        return normalized;
    }

    private String resolveRecipientName(User technician) {
        if (technician.getFirstName() != null && !technician.getFirstName().isBlank()) {
            return HtmlUtils.htmlEscape(technician.getFirstName());
        }

        if (technician.getUsername() != null && !technician.getUsername().isBlank()) {
            return HtmlUtils.htmlEscape(technician.getUsername());
        }

        return "Technician";
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? "Not set" : dateTime.format(EMAIL_DATE_TIME_FORMATTER);
    }

    private String resolveCurrentAssignerDisplayName() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
                return "Manager";
            }

            String principal = authentication.getName();

            return userRepository.findByUsername(principal)
                    .map(user -> {
                        if (user.getDisplayName() != null && !user.getDisplayName().isBlank()) return user.getDisplayName();
                        if (user.getFirstName() != null && !user.getFirstName().isBlank()) return user.getFirstName();
                        return user.getUsername();
                    })
                    .orElse(principal);
        } catch (Exception ex) {
            log.warn("Failed to resolve assigner display name: {}", ex.getMessage());
            return "Manager";
        }
    }

    private void validateTaskReferences(TaskRequest request) {
        if (request.getMachineId() != null && !machineRepository.existsById(request.getMachineId())) {
            throw new IllegalArgumentException("Machine not found with id " + request.getMachineId());
        }

        if (request.getMaintenanceId() != null && !maintenanceRepository.existsById(request.getMaintenanceId())) {
            throw new IllegalArgumentException("Maintenance not found with id " + request.getMaintenanceId());
        }
    }

    /**
     * Delete task
     */
    public void deleteTask(Long id) {

        log.info("Deleting task: {}", id);

        if (!taskRepository.existsById(id)) {

            throw new IllegalArgumentException(
                    "Task not found: " + id
            );
        }

        taskRepository.deleteById(id);

        log.info("Task deleted successfully: {}", id);
    }

    /**
     * Get tasks by assigned technician
     */
    @Transactional(readOnly = true)
    public List<Task> getTasksByAssignedTo(String assignedTo) {

        log.debug("Fetching tasks for assigned user: {}", assignedTo);

        return taskRepository.findByAssignedTo(assignedTo, PageRequest.of(0, LIST_CAP)).getContent();
    }

    /**
     * Get tasks by status
     */
    @Transactional(readOnly = true)
    public List<Task> getTasksByStatus(String status) {

        log.debug("Fetching tasks with status: {}", status);

        return taskRepository.findByStatus(status, PageRequest.of(0, LIST_CAP)).getContent();
    }

    /**
     * Get tasks by machine
     */
    @Transactional(readOnly = true)
    public List<Task> getTasksByMachine(Long machineId) {

        log.debug("Fetching tasks for machine: {}", machineId);

        return taskRepository.findByMachineId(machineId, PageRequest.of(0, LIST_CAP)).getContent();
    }

    /**
     * Get tasks due between dates
     */
    @Transactional(readOnly = true)
    public List<Task> getTasksDueBetween(
            LocalDateTime startDate,
            LocalDateTime endDate
    ) {

        log.debug(
                "Fetching tasks due between {} and {}",
                startDate,
                endDate
        );

        return taskRepository.findByDueDateBetween(
                startDate,
                endDate
        );
    }
}
