package com.pfe.predictive.common.service;

import com.pfe.predictive.core.entity.User;
import com.pfe.predictive.data.repository.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
