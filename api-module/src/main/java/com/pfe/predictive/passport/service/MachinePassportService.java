package com.pfe.predictive.passport.service;

import com.pfe.predictive.alert.entity.Alert;
import com.pfe.predictive.alert.repository.AlertRepository;
import com.pfe.predictive.attachment.service.AttachmentService;
import com.pfe.predictive.comment.service.CommentService;
import com.pfe.predictive.core.entity.Maintenance;
import com.pfe.predictive.core.entity.MaintenanceRecommendation;
import com.pfe.predictive.data.repository.MachineRepository;
import com.pfe.predictive.data.repository.MaintenanceRecommendationRepository;
import com.pfe.predictive.data.repository.MaintenanceRepository;
import com.pfe.predictive.data.repository.PredictionRecordRepository;
import com.pfe.predictive.data.repository.portal.WarrantyRepository;
import com.pfe.predictive.common.exception.ResourceNotFoundException;
import com.pfe.predictive.machine.service.MachineQueryService;
import com.pfe.predictive.ml.mapper.PredictionRecordMapper;
import com.pfe.predictive.passport.dto.MachinePassportResponse;
import com.pfe.predictive.timeline.service.TimelineService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Digital Machine Passport (Priority 3). Pure composition layer — every
 * field is fetched from the service/repository that already owns that data;
 * nothing here recomputes anything the rest of the app already computes.
 * See MachinePassportResponse's own Javadoc for the full rationale.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MachinePassportService {

    private static final int MAINTENANCE_HISTORY_CAP = 50;
    private static final int RECOMMENDATION_HISTORY_CAP = 20;

    private final MachineQueryService machineQueryService;
    private final MachineRepository machineRepository;
    private final PredictionRecordRepository predictionRecordRepository;
    private final PredictionRecordMapper predictionRecordMapper;
    private final AlertRepository alertRepository;
    private final MaintenanceRecommendationRepository recommendationRepository;
    private final MaintenanceRepository maintenanceRepository;
    private final AttachmentService attachmentService;
    private final CommentService commentService;
    private final WarrantyRepository warrantyRepository;
    private final TimelineService timelineService;

    public MachinePassportResponse forMachine(Long machineId) {
        if (!machineRepository.existsById(machineId)) {
            throw new ResourceNotFoundException("Machine not found with id " + machineId);
        }

        return MachinePassportResponse.builder()
                .machine(machineQueryService.findById(machineId).orElse(null))
                .latestPrediction(latestPrediction(machineId))
                .activeAlerts(activeAlerts(machineId))
                .recommendations(recommendations(machineId))
                .maintenanceHistory(maintenanceHistory(machineId))
                .attachments(attachmentService.list("MACHINE", machineId))
                .comments(commentService.list("MACHINE", machineId))
                .warranties(warrantyRepository.findByMachineIdOrderByEndDateDesc(machineId))
                .timeline(timelineService.forMachine(machineId))
                .build();
    }

    private com.pfe.predictive.ml.dto.PredictionRecordDetailDTO latestPrediction(Long machineId) {
        return predictionRecordRepository.findMostRecentByMachineId(machineId, PageRequest.of(0, 1))
                .getContent().stream()
                .findFirst()
                .map(predictionRecordMapper::toDetailDTO)
                .orElse(null);
    }

    private List<MachinePassportResponse.AlertSummary> activeAlerts(Long machineId) {
        return alertRepository.findByMachineId(machineId, PageRequest.of(0, 100)).getContent().stream()
                .filter(Alert::isActionable)
                .map(alert -> MachinePassportResponse.AlertSummary.builder()
                        .id(alert.getId())
                        .title(alert.getTitle())
                        .severity(alert.getSeverity() != null ? alert.getSeverity().name() : null)
                        .status(alert.getStatus() != null ? alert.getStatus().name() : null)
                        .createdDate(alert.getCreatedDate())
                        .build())
                .toList();
    }

    private List<MachinePassportResponse.RecommendationSummary> recommendations(Long machineId) {
        return recommendationRepository.findByMachineIdOrderByGeneratedAtDesc(machineId).stream()
                .limit(RECOMMENDATION_HISTORY_CAP)
                .map(this::toRecommendationSummary)
                .toList();
    }

    private MachinePassportResponse.RecommendationSummary toRecommendationSummary(MaintenanceRecommendation rec) {
        return MachinePassportResponse.RecommendationSummary.builder()
                .id(rec.getId())
                .urgencyLevel(rec.getUrgencyLevel())
                .recommendedAction(rec.getRecommendedAction())
                .status(rec.getStatus() != null ? rec.getStatus().name() : null)
                .generatedAt(rec.getGeneratedAt())
                .resultingMaintenanceId(rec.getResultingMaintenanceId())
                .build();
    }

    private List<MachinePassportResponse.MaintenanceSummary> maintenanceHistory(Long machineId) {
        return maintenanceRepository.findByMachineId(machineId, PageRequest.of(0, MAINTENANCE_HISTORY_CAP)).getContent().stream()
                .map(this::toMaintenanceSummary)
                .toList();
    }

    private MachinePassportResponse.MaintenanceSummary toMaintenanceSummary(Maintenance m) {
        return MachinePassportResponse.MaintenanceSummary.builder()
                .id(m.getId())
                .type(m.getType() != null ? m.getType().name() : null)
                .status(m.getStatus() != null ? m.getStatus().name() : null)
                .priority(m.getPriority() != null ? m.getPriority().name() : null)
                .scheduledDate(m.getScheduledDate())
                .completedDate(m.getCompletedDate())
                .assignedTechnicianId(m.getAssignedTechnicianId())
                .build();
    }
}
