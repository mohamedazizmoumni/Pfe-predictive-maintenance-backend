package com.pfe.predictive.task;

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

        return taskRepository.findAll();
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
                log.warn("NO TECHNICIAN ASSIGNED");
                return;
            }

            log.info("==================================================");
            log.info("STARTING EMAIL PROCESS");
            log.info("==================================================");

            log.info("Assigned technician raw value: {}", task.getAssignedTo());

            Long technicianId;

            try {

                technicianId = Long.parseLong(task.getAssignedTo());

                log.info("Parsed technician ID: {}", technicianId);

            } catch (NumberFormatException ex) {

                log.error(
                        "assignedTo IS NOT A VALID NUMERIC ID: {}",
                        task.getAssignedTo()
                );

                return;
            }

            User technician = userRepository.findById(technicianId).orElse(null);

            if (technician == null) {

                log.error("NO USER FOUND WITH ID {}", technicianId);

                return;
            }

            log.info("TECHNICIAN FOUND");
            log.info("Username: {}", technician.getUsername());
            log.info("Email: {}", technician.getEmail());
            log.info("First Name: {}", technician.getFirstName());

            if (technician.getEmail() == null || technician.getEmail().isBlank()) {

                log.error(
                        "TECHNICIAN EMAIL IS EMPTY FOR USER {}",
                        technician.getUsername()
                );

                return;
            }

            String subject = "New Maintenance Task Assigned - #" + task.getId();

            String body = buildAssignmentEmailHtml(task, technician, assignedBy);

            log.info("==================================================");
            log.info("SENDING EMAIL");
            log.info("TO: {}", technician.getEmail());
            log.info("SUBJECT: {}", subject);
            log.info("==================================================");

                emailService.sendHtmlEmail(
                    technician.getEmail(),
                    subject,
                    body
            );

            log.info("==================================================");
                    log.info("EMAIL DISPATCH QUEUED");
            log.info("==================================================");

        } catch (Exception ex) {

            log.error("==================================================");
            log.error("TASK EMAIL FAILED");
            log.error("ERROR TYPE: {}", ex.getClass().getName());
            log.error("ERROR MESSAGE: {}", ex.getMessage(), ex);
            log.error("==================================================");
        }
    }

    private String buildAssignmentEmailHtml(Task task, User technician, String assignedBy) {
        String recipientName = resolveRecipientName(technician);
        String safeTaskTitle = escapeHtmlOrDefault(task.getTitle(), "Maintenance Task");
        String safeTaskDescription = escapeHtmlOrDefault(task.getDescription(), "N/A");
        String safePriority = escapeHtml(normalizeValue(task.getPriority(), "MEDIUM"));
        String safeStatus = escapeHtml(normalizeValue(task.getStatus(), "PENDING"));
        String safeAssignedBy = escapeHtmlOrDefault(assignedBy, "Manager");
        String machineId = task.getMachineId() == null
                ? "Not assigned"
                : HtmlUtils.htmlEscape(String.valueOf(task.getMachineId()));
        String maintenanceId = task.getMaintenanceId() == null
                ? "Not assigned"
                : HtmlUtils.htmlEscape(String.valueOf(task.getMaintenanceId()));
        String dueDate = formatDateTime(task.getDueDate());
        String createdAt = formatDateTime(task.getCreatedAt());
        String taskUrl = buildTaskUrl(task.getId());

        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <meta http-equiv="X-UA-Compatible" content="IE=edge">
                    <title>New Task Assigned</title>
                </head>
                <body style="margin:0; padding:0; background-color:#eef2f7; font-family:Arial, Helvetica, sans-serif; color:#0f172a;">
                    <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="background-color:#eef2f7; width:100%%;">
                        <tr>
                            <td align="center" style="padding:32px 16px;">
                                <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="max-width:640px; width:100%%; border-collapse:collapse; background-color:#ffffff; border-radius:18px; overflow:hidden; box-shadow:0 10px 30px rgba(15, 23, 42, 0.08);">
                                    <tr>
                                        <td style="background-color:#0f172a; padding:26px 30px; color:#ffffff; border-bottom:4px solid %s;">
                                            <div style="font-size:12px; letter-spacing:1.6px; text-transform:uppercase; opacity:0.75;">Predictive Maintenance System</div>
                                            <div style="font-size:26px; line-height:1.2; font-weight:700; margin-top:8px;">New Task Assigned</div>
                                            <div style="font-size:14px; line-height:1.6; margin-top:8px; color:#dbe4f0;">A technician task has been created and is ready for review.</div>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td style="padding:28px 30px 8px 30px;">
                                            <div style="font-size:16px; line-height:1.6; margin:0 0 18px 0;">Hello %s,</div>
                                            <div style="font-size:15px; line-height:1.7; color:#334155; margin:0 0 20px 0;">
                                                A new maintenance task has been assigned to you. The summary below highlights the priority, machine context, and due date so you can take action quickly.
                                            </div>

                                            <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="margin-bottom:22px;">
                                                <tr>
                                                    <td style="padding:0 0 12px 0;">
                                                        <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="border-collapse:separate; border-spacing:0; background-color:#f8fafc; border:1px solid #e2e8f0; border-radius:14px;">
                                                            <tr>
                                                                <td style="padding:18px 18px 8px 18px;">
                                                                    <div style="font-size:12px; letter-spacing:1px; text-transform:uppercase; color:#64748b; margin-bottom:8px;">Task Summary</div>
                                                                    <div style="font-size:22px; line-height:1.35; font-weight:700; color:#0f172a; margin-bottom:12px;">%s</div>
                                                                    <table role="presentation" cellspacing="0" cellpadding="0" border="0" style="margin-bottom:12px;">
                                                                        <tr>
                                                                            <td style="padding:0 8px 8px 0;">%s</td>
                                                                            <td style="padding:0 8px 8px 0;">%s</td>
                                                                        </tr>
                                                                    </table>
                                                                    <div style="font-size:14px; line-height:1.6; color:#475569;">Assigned by %s</div>
                                                                </td>
                                                            </tr>
                                                        </table>
                                                    </td>
                                                </tr>
                                            </table>

                                            <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="margin-bottom:22px;">
                                                <tr>
                                                    <td style="padding:0;">
                                                        <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="border-collapse:separate; border-spacing:0; border:1px solid #e2e8f0; border-radius:14px; overflow:hidden;">
                                                            <tr>
                                                                <td style="padding:16px 18px; background-color:#ffffff; border-bottom:1px solid #e2e8f0;">
                                                                    <div style="font-size:12px; letter-spacing:1px; text-transform:uppercase; color:#64748b; margin-bottom:8px;">Task Details</div>
                                                                    <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="font-size:14px; line-height:1.65; color:#1f2937;">
                                                                        <tr>
                                                                            <td style="padding:6px 0; width:38%%; color:#64748b;">Task ID</td>
                                                                            <td style="padding:6px 0; font-weight:700; color:#0f172a;">%s</td>
                                                                        </tr>
                                                                        <tr>
                                                                            <td style="padding:6px 0; width:38%%; color:#64748b;">Title</td>
                                                                            <td style="padding:6px 0; font-weight:600; color:#0f172a;">%s</td>
                                                                        </tr>
                                                                        <tr>
                                                                            <td style="padding:6px 0; width:38%%; color:#64748b; vertical-align:top;">Description</td>
                                                                            <td style="padding:6px 0; color:#334155; white-space:pre-wrap;">%s</td>
                                                                        </tr>
                                                                        <tr>
                                                                            <td style="padding:6px 0; width:38%%; color:#64748b;">Priority</td>
                                                                            <td style="padding:6px 0;">%s</td>
                                                                        </tr>
                                                                        <tr>
                                                                            <td style="padding:6px 0; width:38%%; color:#64748b;">Status</td>
                                                                            <td style="padding:6px 0;">%s</td>
                                                                        </tr>
                                                                        <tr>
                                                                            <td style="padding:6px 0; width:38%%; color:#64748b;">Created At</td>
                                                                            <td style="padding:6px 0; color:#334155;">%s</td>
                                                                        </tr>
                                                                        <tr>
                                                                            <td style="padding:6px 0; width:38%%; color:#64748b;">Due Date</td>
                                                                            <td style="padding:6px 0; color:#334155;">%s</td>
                                                                        </tr>
                                                                    </table>
                                                                </td>
                                                            </tr>
                                                        </table>
                                                    </td>
                                                </tr>
                                            </table>

                                            <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="margin-bottom:22px;">
                                                <tr>
                                                    <td style="padding:0;">
                                                        <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="border-collapse:separate; border-spacing:0; border:1px solid #fdba74; border-radius:14px; overflow:hidden; background-color:#fff7ed;">
                                                            <tr>
                                                                <td style="padding:16px 18px;">
                                                                    <div style="font-size:12px; letter-spacing:1px; text-transform:uppercase; color:#9a3412; margin-bottom:8px;">Machine Alert</div>
                                                                    <div style="font-size:15px; line-height:1.65; color:#7c2d12; margin-bottom:10px;">Review the machine context below before starting work.</div>
                                                                    <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="font-size:14px; line-height:1.65; color:#7c2d12;">
                                                                        <tr>
                                                                            <td style="padding:4px 0; width:38%%; font-weight:600;">Machine ID</td>
                                                                            <td style="padding:4px 0;">%s</td>
                                                                        </tr>
                                                                        <tr>
                                                                            <td style="padding:4px 0; width:38%%; font-weight:600;">Maintenance ID</td>
                                                                            <td style="padding:4px 0;">%s</td>
                                                                        </tr>
                                                                    </table>
                                                                </td>
                                                            </tr>
                                                        </table>
                                                    </td>
                                                </tr>
                                            </table>

                                            <table role="presentation" cellspacing="0" cellpadding="0" border="0" style="margin:26px 0 18px 0;">
                                                <tr>
                                                    <td align="center" style="border-radius:12px; background-color:#2563eb;">
                                                        <a href="%s" target="_blank" rel="noopener noreferrer" style="display:inline-block; padding:14px 24px; color:#ffffff; text-decoration:none; font-size:15px; font-weight:700; letter-spacing:0.2px;">Open Task</a>
                                                    </td>
                                                </tr>
                                            </table>

                                            <div style="font-size:14px; line-height:1.7; color:#475569; margin:0 0 8px 0;">
                                                Open the task to update progress, review the machine details, and coordinate the next action.
                                            </div>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td style="padding:18px 30px 28px 30px; background-color:#f8fafc; border-top:1px solid #e2e8f0;">
                                            <div style="font-size:13px; line-height:1.7; color:#64748b; margin-bottom:8px;">Best regards,</div>
                                            <div style="font-size:14px; line-height:1.6; font-weight:700; color:#0f172a; margin-bottom:10px;">Predictive Maintenance System</div>
                                            <div style="font-size:12px; line-height:1.7; color:#94a3b8;">
                                                This message was generated automatically for %s.
                                            </div>
                                        </td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """.formatted(
                getPriorityAccentColor(task.getPriority()),
                recipientName,
                safeTaskTitle,
                buildBadgeHtml(safePriority, getPriorityBadgeColor(task.getPriority())),
                buildBadgeHtml(safeStatus, getStatusBadgeColor(task.getStatus())),
                safeAssignedBy,
                task.getId(),
                safeTaskTitle,
                safeTaskDescription,
                buildBadgeHtml(safePriority, getPriorityBadgeColor(task.getPriority())),
                buildBadgeHtml(safeStatus, getStatusBadgeColor(task.getStatus())),
                createdAt,
                dueDate,
                machineId,
                maintenanceId,
                HtmlUtils.htmlEscape(taskUrl),
                recipientName
        );
    }

    private String buildBadgeHtml(String label, String backgroundColor) {
        return "<span style=\"display:inline-block; padding:6px 12px; border-radius:999px; background-color:" +
                backgroundColor +
                "; color:#ffffff; font-size:12px; font-weight:700; letter-spacing:0.4px; text-transform:uppercase; line-height:1.2;\">" +
                label +
                "</span>";
    }

    private String getPriorityBadgeColor(String priority) {
        return switch (normalizeValue(priority, "MEDIUM")) {
            case "LOW" -> "#16a34a";
            case "MEDIUM" -> "#d97706";
            case "HIGH" -> "#ea580c";
            case "URGENT" -> "#dc2626";
            default -> "#475569";
        };
    }

    private String getPriorityAccentColor(String priority) {
        return switch (normalizeValue(priority, "MEDIUM")) {
            case "LOW" -> "#16a34a";
            case "MEDIUM" -> "#d97706";
            case "HIGH" -> "#ea580c";
            case "URGENT" -> "#dc2626";
            default -> "#475569";
        };
    }

    private String getStatusBadgeColor(String status) {
        return switch (normalizeValue(status, "PENDING")) {
            case "PENDING" -> "#475569";
            case "IN_PROGRESS" -> "#2563eb";
            case "COMPLETED" -> "#16a34a";
            case "CANCELLED" -> "#dc2626";
            default -> "#334155";
        };
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

    private String normalizeValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim().toUpperCase();
    }

    private String escapeHtml(String value) {
        return HtmlUtils.htmlEscape(value == null ? "" : value.trim());
    }

    private String escapeHtmlOrDefault(String value, String fallback) {
        return HtmlUtils.htmlEscape(value == null || value.isBlank() ? fallback : value.trim());
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? "Not set" : HtmlUtils.htmlEscape(dateTime.format(EMAIL_DATE_TIME_FORMATTER));
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

        return taskRepository.findByAssignedTo(assignedTo);
    }

    /**
     * Get tasks by status
     */
    @Transactional(readOnly = true)
    public List<Task> getTasksByStatus(String status) {

        log.debug("Fetching tasks with status: {}", status);

        return taskRepository.findByStatus(status);
    }

    /**
     * Get tasks by machine
     */
    @Transactional(readOnly = true)
    public List<Task> getTasksByMachine(Long machineId) {

        log.debug("Fetching tasks for machine: {}", machineId);

        return taskRepository.findByMachineId(machineId);
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