package com.pfe.predictive.alert.mapper;

import com.pfe.predictive.alert.dto.*;
import com.pfe.predictive.alert.entity.Alert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * AlertMapper - Converts between Alert entity and DTOs.
 *
 * Purpose:
 * - Entity ↔ DTO conversions for API request/response handling
 * - Standardize mapping logic to avoid duplication in controllers
 * - Support different mapping strategies (full, compact, summary)
 *
 * Usage:
 * - mapper.toResponse(alert) - Convert entity to full response
 * - mapper.toDto(alert) - Convert entity to summary DTO
 * - mapper.toEntity(request) - Convert request to entity
 * - mapper.toResponsePage(page) - Convert page of entities to page of responses
 *
 * @author Alert Module
 * @version 1.0
 */
@Component
public class AlertMapper {

    /**
     * Convert Alert entity to full AlertResponse DTO.
     *
     * @param alert the alert entity
     * @return populated AlertResponse with all fields
     */
    public AlertResponse toResponse(Alert alert) {
        if (alert == null) {
            return null;
        }

        return AlertResponse.builder()
            .id(alert.getId())
            .machineId(alert.getMachineId())
            .title(alert.getTitle())
            .message(alert.getMessage())
            .severity(alert.getSeverity())
            .status(alert.getStatus())
            .category(alert.getCategory())
            .sourceReference(alert.getSourceReference())
            .viewed(alert.getViewed())
            .assignedTo(alert.getAssignedTo())
            .assignedToDisplayName(deriveDisplayName(alert.getAssignedTo()))
            .createdBy(alert.getCreatedBy())
            .createdByDisplayName(deriveDisplayName(alert.getCreatedBy()))
            .createdDate(alert.getCreatedDate())
            .acknowledgedBy(alert.getAcknowledgedBy())
            .acknowledgedByDisplayName(deriveDisplayName(alert.getAcknowledgedBy()))
            .acknowledgedDate(alert.getAcknowledgedDate())
            .escalatedBy(alert.getEscalatedBy())
            .escalatedByDisplayName(deriveDisplayName(alert.getEscalatedBy()))
            .escalatedDate(alert.getEscalatedDate())
            .escalationNotes(alert.getEscalationNotes())
            .closedBy(alert.getClosedBy())
            .closedByDisplayName(deriveDisplayName(alert.getClosedBy()))
            .closedDate(alert.getClosedDate())
            .resolutionNotes(alert.getResolutionNotes())
            .recommendations(alert.getRecommendations())
            .lastModifiedDate(alert.getLastModifiedDate())
            .build();
    }

    /**
     * Convert Alert entity to compact AlertDto (for lists/summaries).
     *
     * @param alert the alert entity
     * @return compact AlertDto with key fields only
     */
    public AlertDto toDto(Alert alert) {
        if (alert == null) {
            return null;
        }

        return AlertDto.builder()
            .id(alert.getId())
            .machineId(alert.getMachineId())
            .title(alert.getTitle())
            .severity(alert.getSeverity())
            .status(alert.getStatus())
            .assignedTo(alert.getAssignedTo())
            .createdDate(alert.getCreatedDate())
            .viewed(alert.getViewed())
            .build();
    }

    /**
     * Convert CreateAlertRequest to Alert entity (for creation).
     *
     * @param request the create alert request
     * @param createdBy the username of who created this alert
     * @return new Alert entity with populated fields
     */
    public Alert toEntity(CreateAlertRequest request, String createdBy) {
        if (request == null) {
            return null;
        }

        return Alert.builder()
            .machineId(request.getMachineId())
            .title(request.getTitle())
            .message(request.getMessage())
            .severity(request.getSeverity())
            .category(request.getCategory())
            .sourceReference(request.getSourceReference())
            .assignedTo(request.getAssignedTo())
            .createdBy(createdBy)
            .recommendations(request.getRecommendations())
            .status(com.pfe.predictive.alert.entity.AlertStatus.NEW)
            .viewed(false)
            .build();
    }

    /**
     * Convert Alert entity list to AlertResponse list.
     *
     * @param alerts list of alert entities
     * @return list of AlertResponse DTOs
     */
    public List<AlertResponse> toResponseList(List<Alert> alerts) {
        if (alerts == null) {
            return List.of();
        }

        return alerts.stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    /**
     * Convert Alert entity list to compact AlertDto list.
     *
     * @param alerts list of alert entities
     * @return list of compact AlertDtos
     */
    public List<AlertDto> toDtoList(List<Alert> alerts) {
        if (alerts == null) {
            return List.of();
        }

        return alerts.stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    /**
     * Convert Page of Alert entities to Page of AlertResponse DTOs.
     *
     * @param page paginated alerts
     * @return paginated AlertResponse DTOs
     */
    public Page<AlertResponse> toResponsePage(Page<Alert> page) {
        if (page == null) {
            return Page.empty();
        }

        List<AlertResponse> content = page.getContent().stream()
            .map(this::toResponse)
            .collect(Collectors.toList());

        return new PageImpl<>(content, page.getPageable(), page.getTotalElements());
    }

    /**
     * Convert Page of Alert entities to Page of compact AlertDtos.
     *
     * @param page paginated alerts
     * @return paginated compact AlertDtos
     */
    public Page<AlertDto> toDtoPage(Page<Alert> page) {
        if (page == null) {
            return Page.empty();
        }

        List<AlertDto> content = page.getContent().stream()
            .map(this::toDto)
            .collect(Collectors.toList());

        return new PageImpl<>(content, page.getPageable(), page.getTotalElements());
    }

    /**
     * Convert Alert to CriticalAlertResponse (for critical alerts only).
     *
     * @param alert the alert entity
     * @return CriticalAlertResponse with urgent info only
     */
    public CriticalAlertResponse toCriticalResponse(Alert alert) {
        if (alert == null) {
            return null;
        }

        long minutesSinceCreation = java.time.temporal.ChronoUnit.MINUTES
            .between(alert.getCreatedDate(), java.time.LocalDateTime.now());

        return CriticalAlertResponse.builder()
            .id(alert.getId())
            .machineId(alert.getMachineId())
            .title(alert.getTitle())
            .createdDate(alert.getCreatedDate())
            .assignedTo(alert.getAssignedTo())
            .recommendations(alert.getRecommendations())
            .minutesSinceCreation((int) minutesSinceCreation)
            .build();
    }

    /**
     * Map acknowledgment to existing alert entity.
     *
     * @param alert the alert to acknowledge
     * @param acknowledgedBy the technician username
     * @param request the acknowledgment request with additional details
     * @return updated alert entity
     */
    public Alert mapAcknowledgment(Alert alert, String acknowledgedBy, AcknowledgeAlertRequest request) {
        if (alert == null) {
            return null;
        }

        alert.setAcknowledgedBy(acknowledgedBy);

        LocalDateTime acknowledgedDate = Optional.ofNullable(request)
            .map(AcknowledgeAlertRequest::getAcknowledgedDate)
            .map(date -> date.atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime())
            .orElse(LocalDateTime.now());

        alert.setAcknowledgedDate(acknowledgedDate);
        alert.setStatus(com.pfe.predictive.alert.entity.AlertStatus.ACKNOWLEDGED);
        alert.setViewed(true);
        return alert;
    }

    /**
     * Map escalation to existing alert entity.
     *
     * @param alert the alert to escalate
     * @param escalatedBy the manager/admin username
     * @param request the escalation request
     * @return updated alert entity
     */
    public Alert mapEscalation(Alert alert, String escalatedBy, EscalateAlertRequest request) {
        if (alert == null) {
            return null;
        }

        alert.setEscalatedBy(escalatedBy);
        alert.setEscalatedDate(LocalDateTime.now());
        alert.setStatus(com.pfe.predictive.alert.entity.AlertStatus.ESCALATED);
        alert.setViewed(true);

        if (request.getReassignTo() != null && StringUtils.hasText(request.getReassignTo().trim())) {
            alert.setAssignedTo(request.getReassignTo().trim());
        }

        if (StringUtils.hasText(request.getEscalationNotes())) {
            alert.setEscalationNotes(request.getEscalationNotes().trim());
        }

        return alert;
    }

    /**
     * Map closure to existing alert entity.
     *
     * @param alert the alert to close
     * @param closedBy the manager/admin username
     * @param request the close request with resolution notes
     * @return updated alert entity
     */
    public Alert mapClosure(Alert alert, String closedBy, CloseAlertRequest request) {
        if (alert == null) {
            return null;
        }

        alert.setClosedBy(closedBy);
        alert.setClosedDate(LocalDateTime.now());
        alert.setStatus(com.pfe.predictive.alert.entity.AlertStatus.CLOSED);
        alert.setResolutionNotes(request.getResolutionNotes());
        alert.setIsActive(false);
        return alert;
    }

    private String deriveDisplayName(String username) {
        if (!StringUtils.hasText(username)) {
            return null;
        }

        String normalized = username.replace('.', ' ').replace('_', ' ').replace('-', ' ').trim();
        if (normalized.isEmpty()) {
            return username;
        }

        String[] parts = normalized.split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1).toLowerCase());
            }
        }
        return builder.toString();
    }
}
