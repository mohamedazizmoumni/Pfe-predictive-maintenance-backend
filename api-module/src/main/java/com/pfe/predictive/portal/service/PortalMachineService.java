package com.pfe.predictive.portal.service;

import com.pfe.predictive.core.entity.Machine;
import com.pfe.predictive.core.entity.Maintenance;
import com.pfe.predictive.core.entity.PredictionHistory;
import com.pfe.predictive.data.repository.MachineRepository;
import com.pfe.predictive.data.repository.MaintenanceRepository;
import com.pfe.predictive.data.repository.PredictionHistoryRepository;
import com.pfe.predictive.data.repository.portal.CustomerMachineLinkRepository;
import com.pfe.predictive.portal.dto.PortalMachineDetailDto;
import com.pfe.predictive.portal.dto.PortalMachineSummaryDto;
import com.pfe.predictive.portal.dto.PortalMaintenanceHistoryDto;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Everything a customer sees is deliberately plain-language — no raw risk
 * scores, no model internals, no enum names leaking to the UI (see
 * PortalMachineDetailDto). This is a different presentation layer over the
 * same Machine/PredictionHistory/Maintenance data internal roles see, not a
 * separate data model.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PortalMachineService {

    private final MachineRepository machineRepository;
    private final PredictionHistoryRepository predictionHistoryRepository;
    private final MaintenanceRepository maintenanceRepository;
    private final CustomerMachineLinkRepository linkRepository;
    private final PortalAccessService portalAccessService;

    public List<PortalMachineSummaryDto> listMyMachines(Long customerUserId) {
        return linkRepository.findByUserId(customerUserId).stream()
                .map(link -> machineRepository.findById(link.getMachineId()).orElse(null))
                .filter(java.util.Objects::nonNull)
                .map(this::toSummary)
                .toList();
    }

    public PortalMachineDetailDto getMachineDetail(Long customerUserId, Long machineId) {
        portalAccessService.requireLinkedMachine(customerUserId, machineId);
        Machine machine = machineRepository.findById(machineId)
                .orElseThrow(() -> new EntityNotFoundException("Machine not found: " + machineId));

        List<PredictionHistory> recent = predictionHistoryRepository.findTop10ByMachineIdOrderByTimestampDesc(machineId);
        PredictionHistory latest = recent.isEmpty() ? null : recent.get(0);

        return PortalMachineDetailDto.builder()
                .machineId(machine.getId())
                .name(machine.getName())
                .serialNumber(machine.getSerialNumber())
                .location(machine.getLocation())
                .model(machine.getModel())
                .statusLabel(statusLabel(machine.getStatus()))
                .healthScore(healthScore(machine.getRiskScore()))
                .installationDate(machine.getInstallationDate())
                .lastMaintenanceDate(machine.getLastMaintenanceDate())
                .nextMaintenanceDate(machine.getNextMaintenanceDate())
                .riskSummary(latest != null ? riskSummary(latest.getRiskLevel()) : "No prediction data yet")
                .recommendedAction(latest != null ? latest.getRecommendedAction() : null)
                .predictionEstimated(latest != null && isEstimated(latest.getModelVersion()))
                .predictionUpdatedAt(latest != null ? latest.getTimestamp() : null)
                .build();
    }

    public Page<PortalMaintenanceHistoryDto> getMaintenanceHistory(Long customerUserId, Long machineId, Pageable pageable) {
        portalAccessService.requireLinkedMachine(customerUserId, machineId);
        return maintenanceRepository.findByMachineId(machineId, pageable)
                .map(this::toHistoryDto);
    }

    private PortalMachineSummaryDto toSummary(Machine machine) {
        return PortalMachineSummaryDto.builder()
                .machineId(machine.getId())
                .name(machine.getName())
                .serialNumber(machine.getSerialNumber())
                .location(machine.getLocation())
                .statusLabel(statusLabel(machine.getStatus()))
                .healthScore(healthScore(machine.getRiskScore()))
                .lastMaintenanceDate(machine.getLastMaintenanceDate())
                .nextMaintenanceDate(machine.getNextMaintenanceDate())
                .build();
    }

    private PortalMaintenanceHistoryDto toHistoryDto(Maintenance maintenance) {
        return PortalMaintenanceHistoryDto.builder()
                .id(maintenance.getId())
                .type(maintenance.getType() != null ? maintenance.getType().name() : null)
                .description(maintenance.getDescription())
                .statusLabel(maintenanceStatusLabel(maintenance.getStatus() != null ? maintenance.getStatus().name() : null))
                .scheduledDate(maintenance.getScheduledDate())
                .completedDate(maintenance.getCompletedDate())
                .build();
    }

    private String statusLabel(String status) {
        if (status == null) return "Unknown";
        return switch (status) {
            case "OPERATIONAL" -> "Running normally";
            case "MAINTENANCE" -> "Under maintenance";
            case "FAULTY" -> "Needs attention";
            default -> status;
        };
    }

    private String maintenanceStatusLabel(String status) {
        if (status == null) return "Unknown";
        return switch (status) {
            case "SCHEDULED" -> "Scheduled";
            case "IN_PROGRESS" -> "In progress";
            case "COMPLETED" -> "Completed";
            case "APPROVED" -> "Completed and approved";
            case "CANCELLED" -> "Cancelled";
            default -> status;
        };
    }

    private String riskSummary(String riskLevel) {
        if (riskLevel == null) return "No prediction data yet";
        return switch (riskLevel) {
            case "LOW" -> "Low risk";
            case "MEDIUM" -> "Moderate risk";
            case "HIGH" -> "Elevated risk — service recommended";
            case "CRITICAL" -> "Urgent — service required";
            default -> riskLevel;
        };
    }

    private Integer healthScore(Double riskScore) {
        double risk = riskScore != null ? riskScore : 0.0;
        return (int) Math.round(Math.max(0.0, Math.min(100.0, 100.0 - risk)));
    }

    private boolean isEstimated(String modelVersion) {
        return modelVersion != null && modelVersion.startsWith("fallback");
    }
}
