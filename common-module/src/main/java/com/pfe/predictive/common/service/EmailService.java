package com.pfe.predictive.common.service;

import com.pfe.predictive.core.entity.User;
import com.pfe.predictive.data.repository.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private static final List<String> ALERT_RECIPIENT_ROLES = List.of("MANAGER", "ADMIN", "SUPER_ADMIN");
    private static final List<String> INQUIRY_RECIPIENT_ROLES = List.of("ADMIN", "SUPER_ADMIN");

    private final JavaMailSender mailSender;
    private final UserRepository userRepository;

    @Value("${app.mail.from:${spring.mail.username}}")
    private String fromAddress;

    @Value("${app.mail.from-name:Sentinel Predictive Maintenance}")
    private String fromName;

    // Where visitor-submitted contact/demo inquiries get emailed. Falls back
    // to every ADMIN/SUPER_ADMIN's own address when unset, same as alerts.
    @Value("${app.mail.contact-inbox:}")
    private String contactInboxAddress;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    @Async
    public void sendSimpleEmail(String to, String subject, String body) {
        sendEmailInternal(to, subject, body, false);
    }

    @Async
    public void sendHtmlEmail(String to, String subject, String htmlBody) {
        sendEmailInternal(to, subject, htmlBody, true);
    }

    @Async
    public void sendEmailToUsersByRoles(List<String> roleNames, String subject, String body) {
        log.info("Queueing email to users with roles {}", roleNames);

        try {
            // Dedupe by email — a user holding more than one of these roles
            // (e.g. SUPER_ADMIN also assigned MANAGER) should only get one email.
            Map<String, User> recipientsByEmail = new LinkedHashMap<>();
            for (String roleName : roleNames) {
                for (User user : userRepository.findUsersByRoleName(roleName)) {
                    if (user.getEmail() != null && !user.getEmail().isBlank()) {
                        recipientsByEmail.putIfAbsent(user.getEmail().toLowerCase(), user);
                    } else {
                        log.warn("Skipping user '{}' because email is empty", user.getUsername());
                    }
                }
            }

            if (recipientsByEmail.isEmpty()) {
                log.warn("No users found with roles {}", roleNames);
                return;
            }

            log.info("Found {} unique recipients for roles {}", recipientsByEmail.size(), roleNames);

            for (User user : recipientsByEmail.values()) {
                sendEmailInternal(user.getEmail(), subject, body, true);
            }

            log.info("Finished processing role-based email queue for roles {}", roleNames);
        } catch (Exception ex) {
            log.error(
                    "Failed to queue emails for roles {}: {}",
                    roleNames,
                    ex.getMessage(),
                    ex
            );
        }
    }

    /**
     * Send a machine-alert notification email built entirely from the given
     * content — every field displayed comes from this one object, so the
     * severity, message, recommended action, and ML analysis can never
     * contradict each other.
     */
    @Async
    public void sendAlertNotification(AlertEmailContent content) {
        String machineLabel = content.machineName() != null && !content.machineName().isBlank()
                ? content.machineName()
                : "Machine #" + content.machineId();

        String subject = "Machine Alert - " + machineLabel;
        String body = buildAlertHtmlEmail(content, machineLabel);

        log.info("Queueing alert emails for machine {}", content.machineId());
        sendEmailToUsersByRoles(ALERT_RECIPIENT_ROLES, subject, body);
    }

    /**
     * Send the "issue resolved" counterpart to sendAlertNotification — the
     * second and last email in an incident's lifecycle. Called exactly once,
     * from AlertService.closeAlert(), which itself can only run once per
     * incident (closing an already-CLOSED alert throws). No scheduler or
     * polling loop calls this, so there is no path that repeats it.
     */
    @Async
    public void sendAlertResolvedNotification(AlertEmailContent content, String resolutionNotes, String closedBy) {
        String machineLabel = content.machineName() != null && !content.machineName().isBlank()
                ? content.machineName()
                : "Machine #" + content.machineId();

        String subject = "Resolved - " + machineLabel;
        String body = buildResolvedHtmlEmail(content, machineLabel, resolutionNotes, closedBy);

        log.info("Queueing resolution email for machine {}", content.machineId());
        sendEmailToUsersByRoles(ALERT_RECIPIENT_ROLES, subject, body);
    }

    /**
     * Notifies admins of a visitor-submitted inquiry from the public
     * marketing site (Contact Us or Request a Demo). Sent to the configured
     * contact inbox if one is set, otherwise to every ADMIN/SUPER_ADMIN —
     * same fallback shape as sendAlertNotification.
     */
    @Async
    public void sendInquiryNotification(String inquiryTypeLabel, String fullName, String email,
                                         String company, String phone, String subject, String message) {
        String subjectLine = "New " + inquiryTypeLabel + " — " + fullName;
        String body = buildInquiryHtmlEmail(inquiryTypeLabel, fullName, email, company, phone, subject, message);

        log.info("Queueing {} notification from {}", inquiryTypeLabel, email);
        if (contactInboxAddress != null && !contactInboxAddress.isBlank()) {
            sendEmailInternal(contactInboxAddress, subjectLine, body, true);
        } else {
            sendEmailToUsersByRoles(INQUIRY_RECIPIENT_ROLES, subjectLine, body);
        }
    }

    /**
     * Tells a user their password was just reset by an administrator —
     * there is no self-service reset flow by design (accounts are
     * admin-provisioned only), so this is the only password-change signal a
     * user ever receives.
     */
    @Async
    public void sendPasswordResetNotification(String toEmail, String recipientName, String resetByUsername) {
        if (toEmail == null || toEmail.isBlank()) {
            log.warn("Skipping password-reset notification: recipient has no email on file");
            return;
        }

        String subject = "Your Sentinel password was reset";
        String body = buildPasswordResetHtmlEmail(recipientName, resetByUsername);

        log.info("Queueing password-reset notification to {}", toEmail);
        sendEmailInternal(toEmail, subject, body, true);
    }

    /**
     * Notifies a technician/user they've been assigned a task or maintenance
     * work order. One shared builder for both entity types (TaskService and
     * MaintenanceController) so a "new task" email and a "new maintenance
     * work order" email look identical apart from their content — same
     * Sentinel-branded shell, same priority-accent-colored layout everything
     * else in this class uses.
     */
    @Async
    public void sendAssignmentNotification(String toEmail, AssignmentEmailContent content) {
        if (toEmail == null || toEmail.isBlank()) {
            log.warn("Skipping assignment notification: recipient has no email on file");
            return;
        }

        String subject = "New " + content.entityTypeLabel() + " Assigned - #" + content.entityId();
        String body = buildAssignmentHtmlEmail(content);

        log.info("Queueing {} assignment notification to {}", content.entityTypeLabel(), toEmail);
        sendEmailInternal(toEmail, subject, body, true);
    }

    /**
     * Notifies MANAGER/ADMIN/SUPER_ADMIN that an overdue maintenance work
     * order was auto-escalated to a higher priority (MaintenanceEscalationService).
     */
    @Async
    public void sendEscalationNotification(EscalationEmailContent content) {
        String subject = "Maintenance #" + content.maintenanceId() + " auto-escalated to " + content.newPriority();
        String body = buildEscalationHtmlEmail(content);

        log.info("Queueing escalation notification for maintenance {}", content.maintenanceId());
        sendEmailToUsersByRoles(ALERT_RECIPIENT_ROLES, subject, body);
    }

    /**
     * Priority 6: sent to the manager who just approved a rapport, with the
     * Priority 5 intervention-report PDF attached. This is the only path in
     * the class that produces a multipart message, since it's the only email
     * that needs a real attachment.
     */
    @Async
    public void sendMaintenanceReportEmail(String toEmail, String recipientName, MaintenanceReportEmailContent content, byte[] pdfBytes) {
        if (toEmail == null || toEmail.isBlank()) {
            log.warn("Skipping maintenance report email: recipient has no email on file");
            return;
        }

        String subject = "Maintenance Report Approved - " + content.machineLabel() + " (Rapport #" + content.rapportId() + ")";
        String body = buildMaintenanceReportHtmlEmail(content, recipientName);
        String filename = "maintenance-report-" + content.rapportId() + ".pdf";

        log.info("Queueing maintenance report email (with PDF) to {}", toEmail);
        sendEmailWithAttachmentInternal(toEmail, subject, body, pdfBytes, filename);
    }

    /**
     * Priority 6: sanitized "maintenance completed" notice for a customer
     * linked to the machine. Deliberately built from a separate template
     * (not sendMaintenanceReportEmail's) and never carries the internal PDF
     * — no cost, technician name, or work-performed detail, per the
     * "do not expose internal information to customers" requirement.
     */
    @Async
    public void sendMaintenanceCompletedCustomerNotification(String toEmail, String recipientName, MaintenanceReportEmailContent content) {
        if (toEmail == null || toEmail.isBlank()) {
            log.warn("Skipping customer maintenance notification: recipient has no email on file");
            return;
        }

        String subject = "Maintenance Completed - " + content.machineLabel();
        String body = buildMaintenanceCompletedCustomerHtmlEmail(content, recipientName);

        log.info("Queueing customer maintenance-completed notification to {}", toEmail);
        sendEmailInternal(toEmail, subject, body, true);
    }

    /**
     * Priority 7: sent to MANAGER/ADMIN/SUPER_ADMIN on a configurable
     * schedule (FleetHealthDigestScheduler) with a snapshot of real
     * fleet-wide KPIs. Reuses sendEmailToUsersByRoles — same recipient
     * resolution/dedupe as escalation and alert notifications, no new
     * broadcast mechanism.
     */
    @Async
    public void sendFleetHealthDigest(FleetHealthDigestEmailContent content) {
        String subject = "Weekly Fleet Health Digest - " + content.periodLabel();
        String body = buildFleetHealthDigestHtmlEmail(content);

        log.info("Queueing weekly fleet health digest ({})", content.periodLabel());
        sendEmailToUsersByRoles(ALERT_RECIPIENT_ROLES, subject, body);
    }

    private String buildFleetHealthDigestHtmlEmail(FleetHealthDigestEmailContent content) {
        String accentColor = "#2563eb";
        StringBuilder html = new StringBuilder();
        openShell(html);
        appendHeader(html, accentColor, "&#128202;");

        html.append("<tr><td style=\"padding:8px 36px 36px;\">");
        html.append(eyebrow("#94a3b8", "Weekly Fleet Health Digest"));
        html.append("<p style=\"margin:6px 0 22px;color:#0f172a;font-size:19px;font-weight:700;letter-spacing:-0.01em;\">")
            .append(escapeHtml(content.periodLabel())).append("</p>");

        html.append("<p style=\"margin:28px 0 10px;color:#94a3b8;font-size:11px;font-weight:700;text-transform:uppercase;letter-spacing:0.06em;\">Fleet Overview</p>");
        html.append(insightGrid(List.of(
                new String[]{"Machines", String.valueOf(content.machineCount())},
                new String[]{"Avg. Fleet Health", content.fleetAverageHealth() != null ? String.format("%.1f%%", content.fleetAverageHealth()) : "Not available"},
                new String[]{"Open Work Orders", String.valueOf(content.openWorkOrders())},
                new String[]{"Overdue Work Orders", String.valueOf(content.overdueWorkOrders())},
                new String[]{"Budget Utilization (YTD)", content.budgetUtilizationPercentage() != null ? content.budgetUtilizationPercentage() + "%" : "No budget set"}
        )));

        html.append("<p style=\"margin:28px 0 10px;color:#94a3b8;font-size:11px;font-weight:700;text-transform:uppercase;letter-spacing:0.06em;\">Alerts This Period</p>");
        html.append(insightGrid(List.of(
                new String[]{"New Alerts", String.valueOf(content.newAlertsThisWeek())},
                new String[]{"Resolved Alerts", String.valueOf(content.resolvedAlertsThisWeek())},
                new String[]{"Currently Unresolved", String.valueOf(content.unresolvedAlerts())},
                new String[]{"Avg. Resolution Time", content.averageResolutionTimeHours() != null ? String.format("%.1f hrs", content.averageResolutionTimeHours()) : "Not available"}
        )));

        html.append("<p style=\"margin:28px 0 10px;color:#94a3b8;font-size:11px;font-weight:700;text-transform:uppercase;letter-spacing:0.06em;\">Maintenance Completed This Period</p>");
        html.append(insightGrid(List.of(
                new String[]{"Preventive", String.valueOf(content.preventiveCompletedThisWeek())},
                new String[]{"Corrective", String.valueOf(content.correctiveCompletedThisWeek())}
        )));

        if (content.topReliabilityRisks() != null && !content.topReliabilityRisks().isEmpty()) {
            html.append("<p style=\"margin:28px 0 10px;color:#94a3b8;font-size:11px;font-weight:700;text-transform:uppercase;letter-spacing:0.06em;\">Top Reliability Risks (lowest MTBF)</p>");
            html.append(calloutOpen("#fff7ed", "#ea580c"));
            for (FleetHealthDigestEmailContent.TopRisk risk : content.topReliabilityRisks()) {
                html.append("<p style=\"margin:0 0 6px;color:#7c2d12;font-size:13.5px;line-height:1.5;\">")
                    .append("&bull; ").append(escapeHtml(risk.machineName()))
                    .append(" &mdash; MTBF ")
                    .append(risk.mtbfHours() != null ? String.format("%.0f hrs", risk.mtbfHours()) : "N/A")
                    .append(", ").append(risk.failureCount() != null ? risk.failureCount() : 0).append(" failure(s)")
                    .append("</p>");
            }
            html.append(calloutClose());
        }

        if (content.actionUrl() != null && !content.actionUrl().isBlank()) {
            html.append(ctaButton(accentColor, "Open Dashboard", content.actionUrl()));
        }

        html.append("</td></tr>");
        appendFooter(html, "This is an automated weekly digest — please do not reply to this email.");
        closeShell(html);

        return html.toString();
    }

    private String buildAssignmentHtmlEmail(AssignmentEmailContent content) {
        String accentColor = priorityAccentColor(content.priority());
        String entityLabel = content.entityTypeLabel() != null ? content.entityTypeLabel() : "Task";

        StringBuilder html = new StringBuilder();
        openShell(html);
        appendHeader(html, accentColor, "&#128203;");

        html.append("<tr><td style=\"padding:8px 36px 36px;\">");
        html.append(eyebrow("#94a3b8", "New " + entityLabel + " Assigned"));
        html.append("<p style=\"margin:6px 0 20px;color:#0f172a;font-size:19px;font-weight:700;letter-spacing:-0.01em;\">")
            .append(escapeHtml(content.title() != null && !content.title().isBlank()
                    ? content.title() : entityLabel + " #" + content.entityId()))
            .append("</p>");

        html.append("<p style=\"margin:0 0 22px;color:#334155;font-size:14.5px;line-height:1.6;\">Hi ")
            .append(escapeHtml(content.recipientName() != null && !content.recipientName().isBlank() ? content.recipientName() : "there"))
            .append(", a new ").append(escapeHtml(entityLabel.toLowerCase())).append(" has been assigned to you")
            .append(content.assignedBy() != null && !content.assignedBy().isBlank()
                    ? " by <strong>" + escapeHtml(content.assignedBy()) + "</strong>" : "")
            .append(". Review the details below and take action when you're ready.</p>");

        if (content.description() != null && !content.description().isBlank()) {
            html.append(calloutOpen("#f8fafc", "#cbd5e1"));
            html.append(eyebrow("#64748b", "Description"));
            html.append("<p style=\"margin:6px 0 0;color:#334155;font-size:14.5px;line-height:1.6;white-space:pre-wrap;\">")
                .append(escapeHtml(content.description())).append("</p>");
            html.append(calloutClose());
        }

        List<String[]> pairs = new java.util.ArrayList<>();
        pairs.add(new String[]{entityLabel + " ID", "#" + content.entityId()});
        pairs.add(new String[]{"Priority", content.priority() != null ? content.priority() : "Not set"});
        pairs.add(new String[]{"Status", content.status() != null ? content.status() : "Not set"});
        if (content.machineId() != null) {
            pairs.add(new String[]{"Machine ID", "#" + content.machineId()});
        }
        if (content.dateLabel() != null && content.dateValue() != null && !content.dateValue().isBlank()) {
            pairs.add(new String[]{content.dateLabel(), content.dateValue()});
        }
        html.append(insightGrid(pairs));

        html.append(ctaButton(accentColor,
                content.actionLabel() != null ? content.actionLabel() : "View " + entityLabel,
                content.actionUrl()));

        html.append("</td></tr>");
        appendFooter(html, "This is an automated notification — please do not reply to this email.");
        closeShell(html);

        return html.toString();
    }

    private String buildEscalationHtmlEmail(EscalationEmailContent content) {
        String accentColor = priorityAccentColor(content.newPriority());
        String machineUrl = machineUrl(content.machineId());

        StringBuilder html = new StringBuilder();
        openShell(html);
        appendHeader(html, accentColor, "&#9888;");

        html.append("<tr><td style=\"padding:8px 36px 36px;\">");
        html.append(eyebrow("#94a3b8", "Overdue Maintenance Escalated"));
        html.append("<p style=\"margin:6px 0 22px;color:#0f172a;font-size:19px;font-weight:700;letter-spacing:-0.01em;\">Work Order #")
            .append(content.maintenanceId()).append("</p>");

        html.append(calloutOpen("#fff7ed", accentColor));
        html.append("<p style=\"margin:0;color:#7c2d12;font-size:14.5px;line-height:1.6;\">This work order has been open past the configured overdue threshold (")
            .append(content.overdueThresholdDays()).append(" day(s)) and was escalated automatically from ")
            .append(pill(priorityAccentColor(content.previousPriority()), content.previousPriority()))
            .append(" to ").append(pill(accentColor, content.newPriority())).append(".</p>");
        html.append(calloutClose());

        html.append(insightGrid(List.of(
                new String[]{"Machine ID", "#" + content.machineId()},
                new String[]{"Was Scheduled For", content.scheduledDate() != null ? content.scheduledDate() : "Not set"}
        )));

        html.append(ctaButton(accentColor, "View Machine", machineUrl));

        html.append("</td></tr>");
        appendFooter(html, "This is an automated notification — please do not reply to this email.");
        closeShell(html);

        return html.toString();
    }

    private String priorityAccentColor(String priority) {
        String normalized = priority == null ? "" : priority.toUpperCase();
        return switch (normalized) {
            case "CRITICAL", "URGENT" -> "#dc2626";
            case "HIGH" -> "#ea580c";
            case "MEDIUM" -> "#d97706";
            case "LOW" -> "#16a34a";
            default -> "#2563eb";
        };
    }

    private String buildInquiryHtmlEmail(String inquiryTypeLabel, String fullName, String email,
                                          String company, String phone, String subject, String message) {
        String accentColor = "#2563eb";
        StringBuilder html = new StringBuilder();
        openShell(html);
        appendHeader(html, accentColor, "@");

        html.append("<tr><td style=\"padding:8px 36px 36px;\">");
        html.append(pill(accentColor, inquiryTypeLabel.toUpperCase()));
        html.append("<p style=\"margin:14px 0 22px;color:#0f172a;font-size:19px;font-weight:700;letter-spacing:-0.01em;\">")
            .append(escapeHtml(fullName)).append("</p>");

        html.append(insightGrid(List.of(
                new String[]{"Email", email != null ? email : "Not provided"},
                new String[]{"Phone", phone != null && !phone.isBlank() ? phone : "Not provided"},
                new String[]{"Company", company != null && !company.isBlank() ? company : "Not provided"},
                new String[]{"Subject", subject != null && !subject.isBlank() ? subject : "Not provided"}
        )));

        html.append(calloutOpen("#f8fafc", "#cbd5e1"));
        html.append(eyebrow("#64748b", "Message"));
        html.append("<p style=\"margin:6px 0 0;color:#334155;font-size:14.5px;line-height:1.6;white-space:pre-wrap;\">")
            .append(escapeHtml(message)).append("</p>");
        html.append(calloutClose());

        html.append(ctaButton(accentColor, "Reply by Email", "mailto:" + (email != null ? email : "")));

        html.append("</td></tr>");
        appendFooter(html, "Submitted from the Sentinel public website.");
        closeShell(html);

        return html.toString();
    }

    private String buildPasswordResetHtmlEmail(String recipientName, String resetByUsername) {
        String accentColor = "#2563eb";
        StringBuilder html = new StringBuilder();
        openShell(html);
        appendHeader(html, accentColor, "&#128274;");

        html.append("<tr><td style=\"padding:8px 36px 36px;\">");
        html.append("<p style=\"margin:0 0 22px;color:#0f172a;font-size:19px;font-weight:700;letter-spacing:-0.01em;\">Password reset</p>");

        html.append(calloutOpen("#eff6ff", accentColor));
        html.append("<p style=\"margin:0;color:#1e293b;font-size:14.5px;line-height:1.6;\">Hi ")
            .append(escapeHtml(recipientName != null && !recipientName.isBlank() ? recipientName : "there"))
            .append(", your Sentinel account password was just reset by an administrator")
            .append(resetByUsername != null && !resetByUsername.isBlank() ? " (<strong>" + escapeHtml(resetByUsername) + "</strong>)" : "")
            .append(". Use the new password provided to you to sign in.</p>");
        html.append(calloutClose());

        html.append("<p style=\"margin:16px 0 0;color:#64748b;font-size:13px;line-height:1.6;\">If you didn't expect this, contact your administrator right away.</p>");

        html.append(ctaButton(accentColor, "Sign In", frontendUrl != null && !frontendUrl.isBlank() ? frontendUrl + "/auth/login" : "http://localhost:4200/auth/login"));

        html.append("</td></tr>");
        appendFooter(html, "This is an automated notification — please do not reply to this email.");
        closeShell(html);

        return html.toString();
    }

    private String buildMaintenanceReportHtmlEmail(MaintenanceReportEmailContent content, String recipientName) {
        String accentColor = "#2563eb";
        StringBuilder html = new StringBuilder();
        openShell(html);
        appendHeader(html, accentColor, "&#128196;");

        html.append("<tr><td style=\"padding:8px 36px 36px;\">");
        html.append(eyebrow("#94a3b8", "Maintenance Report Ready"));
        html.append("<p style=\"margin:6px 0 20px;color:#0f172a;font-size:19px;font-weight:700;letter-spacing:-0.01em;\">")
            .append(escapeHtml(content.machineLabel())).append(" &mdash; Rapport #").append(content.rapportId())
            .append("</p>");

        html.append("<p style=\"margin:0 0 22px;color:#334155;font-size:14.5px;line-height:1.6;\">Hi ")
            .append(escapeHtml(recipientName != null && !recipientName.isBlank() ? recipientName : "there"))
            .append(", the intervention report for the rapport you just approved is attached as a PDF. A summary is below.</p>");

        if (content.workPerformed() != null && !content.workPerformed().isBlank()) {
            html.append(calloutOpen("#f8fafc", "#cbd5e1"));
            html.append(eyebrow("#64748b", "Work Performed"));
            html.append("<p style=\"margin:6px 0 0;color:#334155;font-size:14.5px;line-height:1.6;white-space:pre-wrap;\">")
                .append(escapeHtml(content.workPerformed())).append("</p>");
            html.append(calloutClose());
        }

        List<String[]> pairs = new java.util.ArrayList<>();
        pairs.add(new String[]{"Technician", content.technicianName() != null ? content.technicianName() : "Not set"});
        pairs.add(new String[]{"Total Cost", content.totalCostLabel() != null ? content.totalCostLabel() : "Not set"});
        pairs.add(new String[]{"Approved By", content.approvedBy() != null ? content.approvedBy() : "Not set"});
        pairs.add(new String[]{"Approved On", content.approvedDateLabel() != null ? content.approvedDateLabel() : "Not set"});
        html.append(insightGrid(pairs));

        if (content.actionUrl() != null && !content.actionUrl().isBlank()) {
            html.append(ctaButton(accentColor, "View Rapport", content.actionUrl()));
        }

        html.append("</td></tr>");
        appendFooter(html, "This is an automated notification — please do not reply to this email.");
        closeShell(html);

        return html.toString();
    }

    /**
     * Deliberately omits work-performed detail, technician name, approver,
     * and cost — a customer sees only that maintenance on their machine is
     * done, not how it was billed or who touched it internally.
     */
    private String buildMaintenanceCompletedCustomerHtmlEmail(MaintenanceReportEmailContent content, String recipientName) {
        String accentColor = "#059669";
        StringBuilder html = new StringBuilder();
        openShell(html);
        appendHeader(html, accentColor, "&#10003;");

        html.append("<tr><td style=\"padding:8px 36px 36px;\">");
        html.append(eyebrow("#94a3b8", "Maintenance Update"));
        html.append("<p style=\"margin:6px 0 20px;color:#0f172a;font-size:19px;font-weight:700;letter-spacing:-0.01em;\">")
            .append(escapeHtml(content.machineLabel())).append("</p>");

        html.append(calloutOpen("#ecfdf5", accentColor));
        html.append(pill(accentColor, "COMPLETED"));
        html.append("<p style=\"margin:10px 0 0;color:#065f46;font-size:14.5px;line-height:1.6;\">Hi ")
            .append(escapeHtml(recipientName != null && !recipientName.isBlank() ? recipientName : "there"))
            .append(", scheduled maintenance on ").append(escapeHtml(content.machineLabel()))
            .append(" has been completed and signed off. No action is needed on your part.</p>");
        html.append(calloutClose());

        html.append("</td></tr>");
        appendFooter(html, "This is an automated notification — please do not reply to this email.");
        closeShell(html);

        return html.toString();
    }

    private void sendEmailWithAttachmentInternal(String to, String subject, String body, byte[] attachmentBytes, String attachmentFilename) {
        if (to == null || to.isBlank()) {
            throw new IllegalArgumentException("Recipient email cannot be blank");
        }
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("Email subject cannot be blank");
        }
        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException("Email body cannot be blank");
        }

        log.info("========================================");
        log.info("SENDING EMAIL WITH ATTACHMENT");
        log.info("FROM       : {}", fromAddress);
        log.info("TO         : {}", to);
        log.info("SUBJECT    : {}", subject);
        log.info("ATTACHMENT : {}", attachmentFilename);

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    mimeMessage,
                    true,
                    StandardCharsets.UTF_8.name()
            );

            helper.setFrom(fromAddress, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true);
            if (attachmentBytes != null && attachmentBytes.length > 0) {
                helper.addAttachment(attachmentFilename, new ByteArrayResource(attachmentBytes));
            }

            mailSender.send(mimeMessage);

            log.info("EMAIL WITH ATTACHMENT SENT SUCCESSFULLY TO '{}' WITH SUBJECT '{}'", to, subject);
        } catch (MailException | MessagingException ex) {
            log.error(
                    "FAILED TO SEND EMAIL WITH ATTACHMENT TO '{}' WITH SUBJECT '{}': {}",
                    to,
                    subject,
                    ex.getMessage(),
                    ex
            );
        } catch (Exception ex) {
            log.error(
                    "UNEXPECTED EMAIL ERROR (ATTACHMENT) TO '{}' WITH SUBJECT '{}': {}",
                    to,
                    subject,
                    ex.getMessage(),
                    ex
            );
        }

        log.info("========================================");
    }

    private void sendEmailInternal(String to, String subject, String body, boolean html) {
        if (to == null || to.isBlank()) {
            throw new IllegalArgumentException("Recipient email cannot be blank");
        }

        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("Email subject cannot be blank");
        }

        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException("Email body cannot be blank");
        }

        log.info("========================================");
        log.info("SENDING EMAIL");
        log.info("FROM    : {}", fromAddress);
        log.info("TO      : {}", to);
        log.info("SUBJECT : {}", subject);
        log.info("FORMAT  : {}", html ? "HTML" : "TEXT");

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    mimeMessage,
                    false,
                    StandardCharsets.UTF_8.name()
            );

            helper.setFrom(fromAddress, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, html);

            mailSender.send(mimeMessage);

            log.info("EMAIL SENT SUCCESSFULLY TO '{}' WITH SUBJECT '{}'", to, subject);
        } catch (MailException | MessagingException ex) {
            log.error(
                    "FAILED TO SEND EMAIL TO '{}' WITH SUBJECT '{}': {}",
                    to,
                    subject,
                    ex.getMessage(),
                    ex
            );
        } catch (Exception ex) {
            log.error(
                    "UNEXPECTED EMAIL ERROR TO '{}' WITH SUBJECT '{}': {}",
                    to,
                    subject,
                    ex.getMessage(),
                    ex
            );
        }

        log.info("========================================");
    }

    private String buildAlertHtmlEmail(AlertEmailContent content, String machineLabel) {
        String severity = content.severityLabel() == null ? "UNKNOWN" : content.severityLabel().toUpperCase();
        String accentColor = switch (severity) {
            case "CRITICAL" -> "#dc2626";
            case "HIGH" -> "#ea580c";
            case "WARNING" -> "#d97706";
            default -> "#2563eb";
        };
        String accentBg = switch (severity) {
            case "CRITICAL" -> "#fef2f2";
            case "HIGH" -> "#fff7ed";
            case "WARNING" -> "#fffbeb";
            default -> "#eff6ff";
        };
        String formattedTimestamp = formatTimestamp(content.timestamp());
        String machineUrl = machineUrl(content.machineId());

        StringBuilder html = new StringBuilder();
        openShell(html);
        appendHeader(html, accentColor, "!");

        html.append("<tr><td style=\"padding:8px 36px 36px;\">");
        html.append("<p style=\"margin:0 0 6px;color:#0f172a;font-size:19px;font-weight:700;letter-spacing:-0.01em;\">")
            .append(escapeHtml(content.title())).append("</p>");
        html.append("<p style=\"margin:0 0 22px;color:#94a3b8;font-size:13px;\">")
            .append(escapeHtml(machineLabel)).append(" &nbsp;&middot;&nbsp; ").append(formattedTimestamp).append("</p>");

        // Severity pill + primary message
        html.append(calloutOpen(accentBg, accentColor));
        html.append(pill(accentColor, severity));
        html.append("<p style=\"margin:10px 0 0;color:#1e293b;font-size:14.5px;line-height:1.6;\">")
            .append(escapeHtml(content.message())).append("</p>");
        html.append(calloutClose());

        // Recommended action callout
        html.append(calloutOpen("#ecfdf5", "#059669"));
        html.append(eyebrow("#059669", "Recommended Action"));
        html.append("<p style=\"margin:6px 0 0;color:#065f46;font-size:14.5px;line-height:1.6;\">")
            .append(escapeHtml(content.recommendedAction())).append("</p>");
        html.append(calloutClose());

        // ML insights — bulletproof 2-column table grid (no flex/grid, safe in Outlook)
        html.append("<p style=\"margin:28px 0 10px;color:#94a3b8;font-size:11px;font-weight:700;text-transform:uppercase;letter-spacing:0.06em;\">ML Model Insights (reference)</p>");
        html.append(insightGrid(List.of(
                new String[]{"Health Score", formatHealthOrNA(content.healthScore())},
                new String[]{"Risk Level", content.riskLevel() != null ? content.riskLevel() : "Not available"},
                new String[]{"Anomaly Probability", formatPercentOrNA(content.anomalyProbability())},
                new String[]{"Failure Probability", formatPercentOrNA(content.failureProbability())},
                new String[]{"Anomaly Type", content.anomalyType() != null ? content.anomalyType() : "Not available"},
                new String[]{"Predicted RUL", content.predictedRUL() != null ? String.format("%.0f cycles", content.predictedRUL()) : "Not available"}
        )));

        html.append(ctaButton(accentColor, "View Machine", machineUrl));

        html.append("</td></tr>");
        appendFooter(html, "This is an automated notification — please do not reply to this email.");
        closeShell(html);

        return html.toString();
    }

    private String buildResolvedHtmlEmail(AlertEmailContent content, String machineLabel, String resolutionNotes, String closedBy) {
        String accentColor = "#059669";
        String accentBg = "#ecfdf5";
        String formattedTimestamp = formatTimestamp(content.timestamp());
        String machineUrl = machineUrl(content.machineId());

        StringBuilder html = new StringBuilder();
        openShell(html);
        appendHeader(html, accentColor, "&#10003;");

        html.append("<tr><td style=\"padding:8px 36px 36px;\">");
        html.append("<p style=\"margin:0 0 6px;color:#0f172a;font-size:19px;font-weight:700;letter-spacing:-0.01em;\">")
            .append(escapeHtml(content.title())).append("</p>");
        html.append("<p style=\"margin:0 0 22px;color:#94a3b8;font-size:13px;\">")
            .append(escapeHtml(machineLabel)).append(" &nbsp;&middot;&nbsp; originally opened ").append(formattedTimestamp).append("</p>");

        html.append(calloutOpen(accentBg, accentColor));
        html.append(pill(accentColor, "RESOLVED"));
        html.append("<p style=\"margin:10px 0 0;color:#065f46;font-size:14.5px;line-height:1.6;\">This issue on ")
            .append(escapeHtml(machineLabel)).append(" has been closed")
            .append(closedBy != null && !closedBy.isBlank() ? " by <strong>" + escapeHtml(closedBy) + "</strong>" : "")
            .append(".</p>");
        html.append(calloutClose());

        if (resolutionNotes != null && !resolutionNotes.isBlank()) {
            html.append(calloutOpen("#f8fafc", "#cbd5e1"));
            html.append(eyebrow("#64748b", "Resolution Notes"));
            html.append("<p style=\"margin:6px 0 0;color:#334155;font-size:14.5px;line-height:1.6;\">")
                .append(escapeHtml(resolutionNotes)).append("</p>");
            html.append(calloutClose());
        }

        html.append(ctaButton(accentColor, "View Machine", machineUrl));

        html.append("</td></tr>");
        appendFooter(html, "This is an automated notification — please do not reply to this email.");
        closeShell(html);

        return html.toString();
    }

    // ==================== SHARED TEMPLATE BUILDING BLOCKS ====================
    // Table-based, inline-styled throughout — no flexbox/grid/box-shadow relied
    // on for layout (the old ML-insights rows used display:flex, which Outlook
    // desktop's Word rendering engine doesn't support at all and would have
    // rendered broken/stacked for anyone reading mail through Outlook).

    private void openShell(StringBuilder html) {
        html.append("<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"></head>");
        html.append("<body style=\"margin:0;padding:0;background-color:#eef1f5;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Arial,sans-serif;\">");
        html.append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background-color:#eef1f5;padding:40px 16px;\"><tr><td align=\"center\">");
        html.append("<table role=\"presentation\" width=\"600\" cellpadding=\"0\" cellspacing=\"0\" style=\"width:600px;max-width:100%;background-color:#ffffff;border-radius:20px;border:1px solid #e8eaed;overflow:hidden;\">");
    }

    private void closeShell(StringBuilder html) {
        html.append("</table>");
        html.append("<p style=\"margin:20px 0 0;color:#a1a8b3;font-size:11.5px;\">Sentinel &middot; Predictive Maintenance Platform</p>");
        html.append("</td></tr></table></body></html>");
    }

    private void appendHeader(StringBuilder html, String accentColor, String glyph) {
        html.append("<tr><td style=\"padding:32px 36px 20px;border-bottom:1px solid #f1f3f5;\">");
        html.append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\"><tr>");
        html.append("<td width=\"44\" valign=\"middle\">");
        html.append("<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\"><tr><td width=\"36\" height=\"36\" align=\"center\" valign=\"middle\" ")
            .append("style=\"width:36px;height:36px;border-radius:10px;background-color:").append(accentColor)
            .append(";color:#ffffff;font-size:17px;font-weight:700;line-height:36px;\">").append(glyph).append("</td></tr></table>");
        html.append("</td>");
        html.append("<td valign=\"middle\" style=\"padding-left:12px;\">")
            .append("<span style=\"color:#0f172a;font-size:15px;font-weight:700;letter-spacing:-0.01em;\">Sentinel</span><br>")
            .append("<span style=\"color:#94a3b8;font-size:11.5px;\">Predictive Maintenance</span>")
            .append("</td>");
        html.append("</tr></table>");
        html.append("</td></tr>");
    }

    private void appendFooter(StringBuilder html, String note) {
        html.append("<tr><td style=\"padding:22px 36px;background-color:#fafbfc;border-top:1px solid #f1f3f5;\">")
            .append("<p style=\"margin:0;color:#a1a8b3;font-size:12px;line-height:1.6;\">")
            .append(escapeHtml(note))
            .append("</p></td></tr>");
    }

    private String calloutOpen(String bg, String borderColor) {
        return "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background-color:" + bg
                + ";border-left:3px solid " + borderColor + ";border-radius:0 10px 10px 0;margin-bottom:16px;\">"
                + "<tr><td style=\"padding:16px 20px;\">";
    }

    private String calloutClose() {
        return "</td></tr></table>";
    }

    private String pill(String color, String text) {
        return "<span style=\"display:inline-block;background-color:" + color
                + ";color:#ffffff;font-size:11px;font-weight:700;padding:4px 11px;border-radius:999px;letter-spacing:0.05em;\">"
                + escapeHtml(text) + "</span>";
    }

    private String eyebrow(String color, String text) {
        return "<p style=\"margin:0;color:" + color + ";font-size:11.5px;font-weight:700;text-transform:uppercase;letter-spacing:0.05em;\">"
                + escapeHtml(text) + "</p>";
    }

    /** 2-column grid of label/value pairs, built as nested tables (no CSS grid) so it survives Outlook. */
    private String insightGrid(List<String[]> pairs) {
        StringBuilder grid = new StringBuilder("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\">");
        for (int i = 0; i < pairs.size(); i += 2) {
            grid.append("<tr>");
            grid.append(insightCell(pairs.get(i)[0], pairs.get(i)[1], i + 1 < pairs.size()));
            if (i + 1 < pairs.size()) {
                grid.append(insightCell(pairs.get(i + 1)[0], pairs.get(i + 1)[1], false));
            }
            grid.append("</tr>");
        }
        grid.append("</table>");
        return grid.toString();
    }

    private String insightCell(String label, String value, boolean hasRightNeighbor) {
        String paddingRight = hasRightNeighbor ? "padding-right:8px;" : "";
        return "<td width=\"50%\" style=\"" + paddingRight + "padding-bottom:8px;\">"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background-color:#f8fafc;border:1px solid #eef0f3;border-radius:8px;\">"
                + "<tr><td style=\"padding:10px 12px;\">"
                + "<p style=\"margin:0 0 3px;color:#94a3b8;font-size:10.5px;font-weight:700;text-transform:uppercase;letter-spacing:0.04em;\">" + escapeHtml(label) + "</p>"
                + "<p style=\"margin:0;color:#1e293b;font-size:13px;font-weight:600;\">" + escapeHtml(value) + "</p>"
                + "</td></tr></table></td>";
    }

    private String ctaButton(String color, String label, String url) {
        return "<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin-top:28px;\"><tr><td>"
                + "<a href=\"" + url + "\" style=\"display:inline-block;background-color:" + color
                + ";color:#ffffff;font-size:13.5px;font-weight:700;text-decoration:none;padding:12px 26px;border-radius:9px;letter-spacing:0.01em;\">"
                + escapeHtml(label) + " &rarr;</a>"
                + "</td></tr></table>";
    }

    private String machineUrl(Long machineId) {
        String base = frontendUrl != null && !frontendUrl.isBlank() ? frontendUrl : "http://localhost:4200";
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return machineId != null ? base + "/equipment/" + machineId + "/visual" : base + "/equipment";
    }

    private String formatTimestamp(java.time.LocalDateTime timestamp) {
        return timestamp != null ? timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "N/A";
    }

    private String formatPercentOrNA(Double value) {
        if (value == null || value == 0.0) {
            return "Not Available";
        }
        return String.format("%.1f%%", value * 100);
    }

    private String formatHealthOrNA(Double value) {
        if (value == null) {
            return "Not Available";
        }
        return String.format("%.1f%%", value);
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
