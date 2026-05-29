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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private static final String MANAGER_ROLE = "MANAGER";

    private final JavaMailSender mailSender;
    private final UserRepository userRepository;

    @Value("${app.mail.from:${spring.mail.username}}")
    private String fromAddress;

    @Async
    public void sendSimpleEmail(String to, String subject, String body) {
        sendEmailInternal(to, subject, body, false);
    }

    @Async
    public void sendHtmlEmail(String to, String subject, String htmlBody) {
        sendEmailInternal(to, subject, htmlBody, true);
    }

    @Async
    public void sendEmailToUsersByRole(String roleName, String subject, String body) {
        String normalizedRoleName = roleName == null || roleName.isBlank() ? MANAGER_ROLE : roleName.trim();

        log.info("Queueing email to users with role '{}'", normalizedRoleName);

        try {
            List<User> users = userRepository.findUsersByRoleName(normalizedRoleName);

            if (users == null || users.isEmpty()) {
                log.warn("No users found with role '{}'", normalizedRoleName);
                return;
            }

            log.info("Found {} recipients for role '{}'", users.size(), normalizedRoleName);

            for (User user : users) {
                String recipient = user.getEmail();

                if (recipient == null || recipient.isBlank()) {
                    log.warn("Skipping user '{}' because email is empty", user.getUsername());
                    continue;
                }

                sendEmailInternal(recipient, subject, body, false);
            }

            log.info("Finished processing role-based email queue for role '{}'", normalizedRoleName);
        } catch (Exception ex) {
            log.error(
                    "Failed to queue emails for role '{}': {}",
                    normalizedRoleName,
                    ex.getMessage(),
                    ex
            );
        }
    }

    @Async
    public void notifyManagersOfMachineAlert(
            Long machineId,
            String severity,
            String alertDescription,
            LocalDateTime timestamp,
            String recommendedAction
    ) {
        String safeSeverity = severity == null || severity.isBlank() ? "UNKNOWN" : severity.trim();
        String safeDescription = alertDescription == null || alertDescription.isBlank()
                ? "N/A"
                : alertDescription.trim();
        String safeRecommendedAction = recommendedAction == null || recommendedAction.isBlank()
                ? "Immediate inspection is recommended."
                : recommendedAction.trim();
        LocalDateTime safeTimestamp = timestamp == null ? LocalDateTime.now() : timestamp;

        String subject = "Machine Alert - Machine #" + machineId;
        String body = buildManagerAlertBody(
                machineId,
                safeSeverity,
                safeDescription,
                safeTimestamp,
                safeRecommendedAction
        );

        log.info("Queueing manager alert emails for machine {}", machineId);
        sendEmailToUsersByRole(MANAGER_ROLE, subject, body);
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

            helper.setFrom(fromAddress);
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

    private String buildManagerAlertBody(
            Long machineId,
            String severity,
            String alertDescription,
            LocalDateTime timestamp,
            String recommendedAction
    ) {
        return """
                Dear Manager,

                An automated alert has been triggered for a machine in your facility.

                ==========================================
                ALERT DETAILS
                ==========================================

                Machine ID  : %d
                Severity    : %s
                Alert       : %s
                Timestamp   : %s

                Recommended Action:
                %s

                Please log in to the Predictive Maintenance Platform
                to review the machine status and take action if needed.

                Best regards,
                Predictive Maintenance System
                """.formatted(
                machineId,
                severity,
                alertDescription,
                timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                recommendedAction
        );
    }
}