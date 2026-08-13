package com.pfe.predictive.passport.dto;

import com.pfe.predictive.core.entity.Attachment;
import com.pfe.predictive.comment.dto.CommentResponse;
import com.pfe.predictive.core.entity.portal.Warranty;
import com.pfe.predictive.machine.dto.MachineDTO;
import com.pfe.predictive.ml.dto.PredictionRecordDetailDTO;
import com.pfe.predictive.timeline.dto.TimelineEntry;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Digital Machine Passport (Priority 3) — a single read-time view composing
 * data that already lives behind separate endpoints (specs, latest
 * prediction, active alerts, recommendation history, maintenance history,
 * attachments, comments, warranty, activity timeline). Pure aggregation: no
 * new business logic, no new source of truth — every field here is a
 * pass-through from the service that already owns it.
 */
@Data
@Builder
public class MachinePassportResponse {

    // Identity + current condition
    private MachineDTO machine;
    private PredictionRecordDetailDTO latestPrediction;
    private List<AlertSummary> activeAlerts;

    // Predictive maintenance
    private List<RecommendationSummary> recommendations;

    // Maintenance
    private List<MaintenanceSummary> maintenanceHistory;

    // Documents & evidence
    private List<Attachment> attachments;
    private List<CommentResponse> comments;

    // Warranty
    private List<Warranty> warranties;

    // Timeline
    private List<TimelineEntry> timeline;

    @Data
    @Builder
    public static class AlertSummary {
        private Long id;
        private String title;
        private String severity;
        private String status;
        private LocalDateTime createdDate;
    }

    @Data
    @Builder
    public static class RecommendationSummary {
        private Long id;
        private String urgencyLevel;
        private String recommendedAction;
        private String status;
        private LocalDateTime generatedAt;
        private Long resultingMaintenanceId;
    }

    @Data
    @Builder
    public static class MaintenanceSummary {
        private Long id;
        private String type;
        private String status;
        private String priority;
        private LocalDateTime scheduledDate;
        private LocalDateTime completedDate;
        private Long assignedTechnicianId;
    }
}
