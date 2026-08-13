package com.pfe.predictive.maintenancecost.service;

import com.pfe.predictive.audit.service.AuditEventService;
import com.pfe.predictive.common.exception.ResourceNotFoundException;
import com.pfe.predictive.core.entity.Machine;
import com.pfe.predictive.core.entity.Maintenance;
import com.pfe.predictive.core.entity.MaintenancePriority;
import com.pfe.predictive.core.entity.MaintenanceRecommendation;
import com.pfe.predictive.core.entity.MaintenanceRecommendationStatus;
import com.pfe.predictive.core.entity.MaintenanceStatus;
import com.pfe.predictive.core.entity.MaintenanceType;
import com.pfe.predictive.data.repository.MachineRepository;
import com.pfe.predictive.data.repository.MaintenanceRecommendationRepository;
import com.pfe.predictive.data.repository.MaintenanceRepository;
import com.pfe.predictive.maintenancecost.dto.SavedRecommendationResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Persists a maintenance recommendation and drives its approve/reject
 * lifecycle. Deliberately independent of {@link MaintenanceRecommendationService}
 * (the existing stateless preview generator) — that service resolves
 * machines against the separate, sparsely-seeded {@code maintenance_cost_machines}
 * table (a different table from the real {@code machines} table the rest of
 * the app uses), so a machineId that's valid everywhere else 404s there for
 * any machine outside its 3 demo rows. This service works against the real
 * {@link Machine} entity instead, so approval can create a real work order
 * for any machine. Cost estimates are intentionally not computed here — that
 * would mean either duplicating CostComparisonService's logic or fixing the
 * cross-table mismatch, both out of scope for this pass.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class MaintenanceRecommendationApprovalService {

    private final MaintenanceRecommendationRepository recommendationRepository;
    private final MachineRepository machineRepository;
    private final MaintenanceRepository maintenanceRepository;
    private final AuditEventService auditEventService;

    /**
     * Same as generateAndSave(), but a no-op (returns empty) if the machine
     * already has a PENDING recommendation — used by the automatic
     * AI-incident-triggered path (MachineStreamingService) so a machine
     * stuck at HIGH/CRITICAL across several telemetry ticks doesn't pile up
     * duplicate recommendations a manager hasn't had a chance to act on yet.
     * Manual generation from the Recommendations page still always creates a
     * new one via generateAndSave() directly - this guard is opt-in.
     */
    public java.util.Optional<SavedRecommendationResponse> generateAndSaveIfNoPending(
            Long machineId, double failureProbability, int daysUntilPredictedFailure, String generatedBy) {
        if (recommendationRepository.existsByMachineIdAndStatus(machineId, MaintenanceRecommendationStatus.PENDING)) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(generateAndSave(machineId, failureProbability, daysUntilPredictedFailure, generatedBy));
    }

    public SavedRecommendationResponse generateAndSave(Long machineId, double failureProbability,
                                                         int daysUntilPredictedFailure, String generatedBy) {
        Machine machine = machineRepository.findById(machineId)
                .orElseThrow(() -> new EntityNotFoundException("Machine not found: " + machineId));

        String urgencyLevel = determineUrgency(failureProbability, daysUntilPredictedFailure);
        String recommendedAction = determineAction(urgencyLevel);
        String justification = buildJustification(urgencyLevel, failureProbability, daysUntilPredictedFailure);

        MaintenanceRecommendation recommendation = MaintenanceRecommendation.builder()
                .machineId(machineId)
                .urgencyLevel(urgencyLevel)
                .recommendedAction(recommendedAction)
                .justification(justification)
                .failureProbability(failureProbability)
                .daysUntilFailure(daysUntilPredictedFailure)
                .status(MaintenanceRecommendationStatus.PENDING)
                .build();

        MaintenanceRecommendation saved = recommendationRepository.save(recommendation);
        auditEventService.record(generatedBy, "RECOMMENDATION_GENERATED", "MaintenanceRecommendation", saved.getId(),
                "machineId=" + machineId + ", urgency=" + urgencyLevel);

        return toResponse(saved, machine.getName());
    }

    @Transactional(readOnly = true)
    public Page<SavedRecommendationResponse> history(MaintenanceRecommendationStatus status, Pageable pageable) {
        Page<MaintenanceRecommendation> page = status != null
                ? recommendationRepository.findByStatusOrderByGeneratedAtDesc(status, pageable)
                : recommendationRepository.findAllByOrderByGeneratedAtDesc(pageable);
        return page.map(rec -> toResponse(rec, machineName(rec.getMachineId())));
    }

    public SavedRecommendationResponse approve(Long id, String decidedBy, String note) {
        MaintenanceRecommendation recommendation = getPendingOrThrow(id);

        Long resultingMaintenanceId = null;
        if (!"MONITOR".equals(recommendation.getRecommendedAction())) {
            Maintenance maintenance = new Maintenance();
            maintenance.setMachineId(recommendation.getMachineId());
            maintenance.setType("PREVENTIVE".equals(recommendation.getRecommendedAction())
                    ? MaintenanceType.PREVENTIVE : MaintenanceType.CORRECTIVE);
            maintenance.setPriority(mapPriority(recommendation.getUrgencyLevel()));
            maintenance.setDescription(recommendation.getJustification());
            maintenance.setStatus(MaintenanceStatus.SCHEDULED);
            maintenance.setScheduledDate(java.time.LocalDateTime.now());
            Maintenance saved = maintenanceRepository.save(maintenance);
            resultingMaintenanceId = saved.getId();
        }

        recommendation.setStatus(MaintenanceRecommendationStatus.APPROVED);
        recommendation.setDecidedBy(decidedBy);
        recommendation.setDecidedAt(java.time.LocalDateTime.now());
        recommendation.setDecisionNote(note);
        recommendation.setResultingMaintenanceId(resultingMaintenanceId);

        MaintenanceRecommendation saved = recommendationRepository.save(recommendation);
        auditEventService.record(decidedBy, "RECOMMENDATION_APPROVED", "MaintenanceRecommendation", saved.getId(),
                resultingMaintenanceId != null ? "maintenanceId=" + resultingMaintenanceId : "action=MONITOR");

        return toResponse(saved, machineName(saved.getMachineId()));
    }

    public SavedRecommendationResponse reject(Long id, String decidedBy, String note) {
        if (note == null || note.isBlank()) {
            throw new IllegalArgumentException("A reason is required to reject a recommendation");
        }
        MaintenanceRecommendation recommendation = getPendingOrThrow(id);
        recommendation.setStatus(MaintenanceRecommendationStatus.REJECTED);
        recommendation.setDecidedBy(decidedBy);
        recommendation.setDecidedAt(java.time.LocalDateTime.now());
        recommendation.setDecisionNote(note);

        MaintenanceRecommendation saved = recommendationRepository.save(recommendation);
        auditEventService.record(decidedBy, "RECOMMENDATION_REJECTED", "MaintenanceRecommendation", saved.getId(), note);

        return toResponse(saved, machineName(saved.getMachineId()));
    }

    private MaintenanceRecommendation getPendingOrThrow(Long id) {
        MaintenanceRecommendation recommendation = recommendationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recommendation not found: " + id));
        if (recommendation.getStatus() != MaintenanceRecommendationStatus.PENDING) {
            throw new IllegalStateException("Recommendation " + id + " is already " + recommendation.getStatus());
        }
        return recommendation;
    }

    private MaintenancePriority mapPriority(String urgencyLevel) {
        try {
            return MaintenancePriority.valueOf(urgencyLevel);
        } catch (IllegalArgumentException ex) {
            return MaintenancePriority.MEDIUM;
        }
    }

    private String determineUrgency(double failureProbability, int daysUntilPredictedFailure) {
        if (failureProbability >= 0.85 || daysUntilPredictedFailure <= 2) return "CRITICAL";
        if (failureProbability >= 0.65 || daysUntilPredictedFailure <= 5) return "HIGH";
        if (failureProbability >= 0.40) return "MEDIUM";
        return "LOW";
    }

    private String determineAction(String urgencyLevel) {
        if ("CRITICAL".equals(urgencyLevel) || "HIGH".equals(urgencyLevel)) return "PREVENTIVE";
        if ("MEDIUM".equals(urgencyLevel)) return "PREVENTIVE";
        return "MONITOR";
    }

    private String buildJustification(String urgencyLevel, double failureProbability, int daysUntilPredictedFailure) {
        String probabilityPercent = BigDecimal.valueOf(failureProbability)
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .toPlainString();
        return String.format("Urgency is %s. Failure probability is %s%% with %d day(s) remaining.",
                urgencyLevel, probabilityPercent, daysUntilPredictedFailure);
    }

    private String machineName(Long machineId) {
        return machineRepository.findById(machineId).map(Machine::getName).orElse(null);
    }

    private SavedRecommendationResponse toResponse(MaintenanceRecommendation rec, String machineName) {
        return SavedRecommendationResponse.builder()
                .id(rec.getId())
                .machineId(rec.getMachineId())
                .machineName(machineName)
                .urgencyLevel(rec.getUrgencyLevel())
                .recommendedAction(rec.getRecommendedAction())
                .justification(rec.getJustification())
                .failureProbability(rec.getFailureProbability())
                .daysUntilFailure(rec.getDaysUntilFailure())
                .status(rec.getStatus())
                .generatedAt(rec.getGeneratedAt())
                .decidedBy(rec.getDecidedBy())
                .decidedAt(rec.getDecidedAt())
                .decisionNote(rec.getDecisionNote())
                .resultingMaintenanceId(rec.getResultingMaintenanceId())
                .build();
    }
}
