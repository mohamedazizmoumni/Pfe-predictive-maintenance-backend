package com.yourpackage.business.service;

import com.pfe.predictive.maintenancecost.entity.Machine;
import com.pfe.predictive.maintenancecost.entity.MaintenanceAction;
import com.pfe.predictive.maintenancecost.entity.MaintenancePart;
import com.pfe.predictive.maintenancecost.enums.MaintenanceActionStatus;
import com.pfe.predictive.maintenancecost.enums.MaintenanceActionType;
import com.pfe.predictive.maintenancecost.repository.MachineRepository;
import com.pfe.predictive.maintenancecost.repository.MaintenanceActionRepository;
import com.pfe.predictive.maintenancecost.repository.MaintenancePartRepository;
import com.yourpackage.business.dto.MaintenanceRecommendationDTO;
import com.yourpackage.business.dto.RecommendationRequestDTO;
import com.yourpackage.business.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Set;

@Service
@Validated
public class MaintenanceRecommendationService {

    private final CostCalculationService costCalculationService;
    private final BudgetService budgetService;
    private final PartAvailabilityService partAvailabilityService;
    private final MachineRepository machineRepository;
    private final MaintenancePartRepository maintenancePartRepository;
    private final MaintenanceActionRepository maintenanceActionRepository;

    public MaintenanceRecommendationService(CostCalculationService costCalculationService,
                                            BudgetService budgetService,
                                            PartAvailabilityService partAvailabilityService,
                                            MachineRepository machineRepository,
                                            MaintenancePartRepository maintenancePartRepository,
                                            MaintenanceActionRepository maintenanceActionRepository) {
        this.costCalculationService = costCalculationService;
        this.budgetService = budgetService;
        this.partAvailabilityService = partAvailabilityService;
        this.machineRepository = machineRepository;
        this.maintenancePartRepository = maintenancePartRepository;
        this.maintenanceActionRepository = maintenanceActionRepository;
    }

    public MaintenanceRecommendationDTO generateRecommendation(@Valid @NotNull RecommendationRequestDTO request) {
        // Step 1
        Machine machine = machineRepository.findById(request.getMachineId())
                .orElseThrow(() -> new ResourceNotFoundException("Machine not found with id " + request.getMachineId()));

        // Step 2
        List<MaintenancePart> requiredParts = maintenancePartRepository.findAllById(request.getRequiredPartIds());

        // Step 3
        MaintenanceAction plannedAction = maintenanceActionRepository.findByMachineOrderByScheduledDateDesc(machine)
                .stream()
                .filter(action -> action.getStatus() == MaintenanceActionStatus.PLANNED)
                .findFirst()
                .orElseGet(() -> MaintenanceAction.builder()
                        .machine(machine)
                        .type(MaintenanceActionType.PREVENTIVE)
                        .estimatedDurationHours(4.0)
                        .laborCostPerHour(BigDecimal.valueOf(120.0))
                        .parts(Set.of())
                        .status(MaintenanceActionStatus.PLANNED)
                        .scheduledDate(java.time.LocalDateTime.now())
                        .build());

        // Step 4
        BigDecimal preventiveCost = costCalculationService.calculatePreventiveCost(plannedAction);

        // Step 5
        double estimatedFailureDowntimeHours = request.getDaysUntilPredictedFailure() * 8.0;
        BigDecimal correctiveCost = costCalculationService.calculateCorrectiveCost(
                machine,
                estimatedFailureDowntimeHours,
                requiredParts
        );

        // Step 6
        BigDecimal savings = costCalculationService.calculateSavings(correctiveCost, preventiveCost);

        // Step 7
        boolean partsAvailable = partAvailabilityService.checkPartsAvailable(requiredParts);
        List<String> missingParts = partAvailabilityService.getMissingPartNames(requiredParts);

        // Step 8
        String urgencyLevel = determineUrgency(request.getFailureProbability(), request.getDaysUntilPredictedFailure());

        // Step 9
        String recommendedAction = determineAction(urgencyLevel, partsAvailable);

        // Step 10
        String justification = buildJustification(
                urgencyLevel,
                request.getFailureProbability(),
                request.getDaysUntilPredictedFailure(),
                savings,
                partsAvailable,
                missingParts
        );

        // Step 11
        return MaintenanceRecommendationDTO.builder()
                .machineId(machine.getId())
                .machineName(machine.getName())
                .urgencyLevel(urgencyLevel)
                .recommendedAction(recommendedAction)
                .justification(justification)
                .estimatedCost(selectEstimatedCost(recommendedAction, preventiveCost, correctiveCost))
                .estimatedSavings(savings)
                .partsAvailable(partsAvailable)
                .missingParts(missingParts)
                .daysUntilFailure(request.getDaysUntilPredictedFailure())
                .failureProbability(request.getFailureProbability())
                .build();
    }

    private String determineUrgency(double failureProbability, int daysUntilPredictedFailure) {
        if (failureProbability >= 0.85 || daysUntilPredictedFailure <= 2) {
            return "CRITICAL";
        }
        if (failureProbability >= 0.65 || daysUntilPredictedFailure <= 5) {
            return "HIGH";
        }
        if (failureProbability >= 0.40) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private String determineAction(String urgencyLevel, boolean partsAvailable) {
        if ("CRITICAL".equals(urgencyLevel) || "HIGH".equals(urgencyLevel)) {
            return "PREVENTIVE";
        }
        if ("MEDIUM".equals(urgencyLevel)) {
            return partsAvailable ? "PREVENTIVE" : "CORRECTIVE";
        }
        return "MONITOR";
    }

    private String buildJustification(String urgencyLevel,
                                      double failureProbability,
                                      int daysUntilPredictedFailure,
                                      BigDecimal savings,
                                      boolean partsAvailable,
                                      List<String> missingParts) {
        String probabilityPercent = BigDecimal.valueOf(failureProbability)
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .toPlainString();

        String partsStatus = partsAvailable
                ? "All required parts are in stock."
                : "Missing parts: " + String.join(", ", missingParts) + ".";

        return "Urgency is " + urgencyLevel + ". " +
                "Failure probability is " + probabilityPercent + "% with " + daysUntilPredictedFailure + " days remaining. " +
                "Preventive action saves EUR " + savings.setScale(2, RoundingMode.HALF_UP).toPlainString() + " vs corrective repair. " +
                partsStatus;
    }

    private BigDecimal selectEstimatedCost(String action, BigDecimal preventiveCost, BigDecimal correctiveCost) {
        return switch (action) {
            case "PREVENTIVE" -> preventiveCost;
            case "CORRECTIVE" -> correctiveCost;
            default -> BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        };
    }
}