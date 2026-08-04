package com.pfe.predictive.timeline.service;

import com.pfe.predictive.alert.entity.Alert;
import com.pfe.predictive.alert.repository.AlertRepository;
import com.pfe.predictive.core.entity.AuditEvent;
import com.pfe.predictive.core.entity.Comment;
import com.pfe.predictive.core.entity.Maintenance;
import com.pfe.predictive.data.repository.AuditEventRepository;
import com.pfe.predictive.data.repository.CommentRepository;
import com.pfe.predictive.data.repository.MaintenanceRepository;
import com.pfe.predictive.timeline.dto.TimelineEntry;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Aggregates a machine's history — maintenance events, audit trail, alerts,
 * comments — into one chronological feed. Read-only view, no new storage;
 * pulls from tables that already exist.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TimelineService {

    private static final int LOOKBACK_CAP = 300;

    private final MaintenanceRepository maintenanceRepository;
    private final AuditEventRepository auditEventRepository;
    private final AlertRepository alertRepository;
    private final CommentRepository commentRepository;

    public List<TimelineEntry> forMachine(Long machineId) {
        List<TimelineEntry> entries = new ArrayList<>();

        List<Maintenance> maintenanceList = maintenanceRepository
                .findByMachineId(machineId, PageRequest.of(0, LOOKBACK_CAP))
                .getContent();

        List<Long> maintenanceIds = maintenanceList.stream().map(Maintenance::getId).toList();

        for (Maintenance m : maintenanceList) {
            if (m.getScheduledDate() != null) {
                entries.add(TimelineEntry.builder()
                        .timestamp(m.getScheduledDate())
                        .category("MAINTENANCE_SCHEDULED")
                        .title("Maintenance scheduled — " + m.getType())
                        .description(m.getDescription())
                        .actor(null)
                        .build());
            }
            if (m.getCompletedDate() != null) {
                entries.add(TimelineEntry.builder()
                        .timestamp(m.getCompletedDate())
                        .category("MAINTENANCE_COMPLETED")
                        .title("Maintenance completed — " + m.getType())
                        .description(m.getDescription())
                        .actor(null)
                        .build());
            }
        }

        if (!maintenanceIds.isEmpty()) {
            List<AuditEvent> auditEvents = auditEventRepository.findByEntityTypeAndEntityIdIn("Maintenance", maintenanceIds);
            for (AuditEvent event : auditEvents) {
                entries.add(TimelineEntry.builder()
                        .timestamp(event.getCreatedAt())
                        .category("AUDIT")
                        .title(humanizeAction(event.getAction()))
                        .description(event.getDetails())
                        .actor(event.getActor())
                        .build());
            }

            List<Comment> maintenanceComments = commentRepository.findByEntityTypeAndEntityIdIn("MAINTENANCE", maintenanceIds);
            for (Comment comment : maintenanceComments) {
                entries.add(TimelineEntry.builder()
                        .timestamp(comment.getCreatedAt())
                        .category("COMMENT")
                        .title("Comment on maintenance #" + comment.getEntityId())
                        .description(comment.getBody())
                        .actor(comment.getAuthorUsername())
                        .build());
            }
        }

        List<Alert> alerts = alertRepository.findByMachineId(machineId, PageRequest.of(0, LOOKBACK_CAP)).getContent();
        for (Alert alert : alerts) {
            entries.add(TimelineEntry.builder()
                    .timestamp(alert.getCreatedDate())
                    .category("ALERT")
                    .title(alert.getSeverity() + " — " + alert.getTitle())
                    .description(alert.getMessage())
                    .actor(alert.getCreatedBy())
                    .build());
        }

        for (Comment comment : commentRepository.findByEntityTypeAndEntityIdOrderByCreatedAtAsc("MACHINE", machineId)) {
            entries.add(TimelineEntry.builder()
                    .timestamp(comment.getCreatedAt())
                    .category("COMMENT")
                    .title("Comment on machine")
                    .description(comment.getBody())
                    .actor(comment.getAuthorUsername())
                    .build());
        }

        return entries.stream()
                .filter(e -> e.getTimestamp() != null)
                .sorted(Comparator.comparing(TimelineEntry::getTimestamp).reversed())
                .toList();
    }

    private String humanizeAction(String action) {
        if (action == null) return "Event";
        return action.replace('_', ' ');
    }
}
