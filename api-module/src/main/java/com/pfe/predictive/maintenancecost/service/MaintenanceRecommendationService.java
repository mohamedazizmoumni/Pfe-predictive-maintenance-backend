package com.pfe.predictive.maintenancecost.service;

import com.pfe.predictive.maintenancecost.dto.MaintenanceRecommendationResponse;
import com.pfe.predictive.maintenancecost.entity.Machine;
import com.pfe.predictive.maintenancecost.entity.MaintenanceAction;
import com.pfe.predictive.maintenancecost.entity.MaintenancePart;
import com.pfe.predictive.maintenancecost.enums.MaintenanceActionStatus;
import com.pfe.predictive.maintenancecost.enums.MaintenanceActionType;
import com.pfe.predictive.maintenancecost.repository.MachineRepository;
import com.pfe.predictive.maintenancecost.repository.MaintenanceActionRepository;
import com.pfe.predictive.maintenancecost.repository.MaintenancePartRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Set;

/**
 * Maintenance Recommendation Service
 * Generates smart maintenance recommendations based on:
 * - Failure probability
 * - Days until predicted failure
 * - Parts availability
 * - Cost comparison
 * 
 * @author Finance Module
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MaintenanceRecommendationService {

    private final CostComparisonService costComparisonService;
    private final MachineRepository machineRepository;
    private final MaintenancePartRepository maintenancePartRepository;
    private final MaintenanceActionRepository maintenanceActionRepository;

    /**
     * Generate maintenance recommendation
     * 
     * @param machineId Machine ID
     * @param failureProbability Probability of failure (0.0 - 1.0)
     * @param daysUntilPredictedFailure Days until predicted failure
     * @param requiredPartIds IDs of parts needed
     * @return Comprehensive recommendation
     */
    public MaintenanceRecommendationResponse generateRecommendation(
            Long machineId,
            double failureProbability,
            int daysUntilPredictedFailure,
            List<Long> requiredPartIds) {
        
        log.info("Generating recommendation - Machine: {}, Probability: {}, Days: {}", 
                 machineId, failureProbability, daysUntilPredictedFailure);

        // Step 1: Get machine
        Machine machine = machineRepository.findById(machineId)
                .orElseThrow(() -> new EntityNotFoundException("Machine not found: " + machineId));

        // Step 2: Get required parts
        List<MaintenancePart> requiredParts = requiredPartIds == null || requiredPartIds.isEmpty()
                ? List.of()
                : maintenancePartRepository.findAllById(requiredPartIds);

        // Step 3: Get or create planned action
        MaintenanceAction plannedAction = maintenanceActionRepository
                .findByMachineOrderByScheduledDateDesc(machine)
                .stream()
                .filter(action -> action.getStatus() == MaintenanceActionStatus.PLANNED)
                .findFirst()
                .orElseGet(() -> createDefaultAction(machine, requiredParts));

        // Step 4: Calculate costs
        BigDecimal preventiveCost = costComparisonService.calculatePreventiveCost(plannedAction);
        
        double estimatedFailureDowntimeHours = daysUntilPredictedFailure * 8.0;
        BigDecimal correctiveCost = costComparisonService.calculateCorrectiveCost(
                machine,
                estimatedFailureDowntimeHours,
                requiredParts
        );

        BigDecimal savings = costComparisonService.calculateSavings(correctiveCost, preventiveCost);

        // Step 5: Check parts availability
        boolean partsAvailable = checkPartsAvailable(requiredParts);
        List<String> missingParts = getMissingPartNames(requiredParts);

        // Step 6: Determine urgency
        String urgencyLevel = determineUrgency(failureProbability, daysUntilPredictedFailure);

        // Step 7: Determine recommended action
        String recommendedAction = determineAction(urgencyLevel, partsAvailable);

        // Step 8: Build justification
        String justification = buildJustification(
                urgencyLevel,
                failureProbability,
                daysUntilPredictedFailure,
                savings,
                partsAvailable,
                missingParts
        );

        log.info("Recommendation generated - Urgency: {}, Action: {}, Savings: {}", 
                 urgencyLevel, recommendedAction, savings);

        return MaintenanceRecommendationResponse.builder()
                .machineId(machine.getId())
                .machineName(machine.getName())
                .urgencyLevel(urgencyLevel)
                .recommendedAction(recommendedAction)
                .justification(justification)
                .estimatedCost(selectEstimatedCost(recommendedAction, preventiveCost, correctiveCost))
                .estimatedSavings(savings)
                .partsAvailable(partsAvailable)
                .missingParts(missingParts)
                .daysUntilFailure(daysUntilPredictedFailure)
                .failureProbability(failureProbability)
                .build();
    }

    /**
     * Determine urgency level based on failure probability and time
     * 
     * CRITICAL: Probability >= 85% OR <= 2 days
     * HIGH: Probability >= 65% OR <= 5 days
     * MEDIUM: Probability >= 40%
     * LOW: Everything else
     */
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

    /**
     * Determine recommended action based on urgency and parts availability
     * 
     * CRITICAL/HIGH: Always PREVENTIVE
     * MEDIUM: PREVENTIVE if parts available, else CORRECTIVE
     * LOW: MONITOR
     */
    private String determineAction(String urgencyLevel, boolean partsAvailable) {
        if ("CRITICAL".equals(urgencyLevel) || "HIGH".equals(urgencyLevel)) {
            return "PREVENTIVE";
        }
        if ("MEDIUM".equals(urgencyLevel)) {
            return partsAvailable ? "PREVENTIVE" : "CORRECTIVE";
        }
        return "MONITOR";
    }

    /**
     * Build detailed justification text
     */
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

        return String.format(
            "Urgency is %s. Failure probability is %s%% with %d days remaining. " +
            "Preventive action saves €%s vs corrective repair. %s",
            urgencyLevel,
            probabilityPercent,
            daysUntilPredictedFailure,
            savings.setScale(2, RoundingMode.HALF_UP).toPlainString(),
            partsStatus
        );
    }

    /**
     * Select estimated cost based on recommended action
     */
    private BigDecimal selectEstimatedCost(String action, BigDecimal preventiveCost, BigDecimal correctiveCost) {
        return switch (action) {
            case "PREVENTIVE" -> preventiveCost;
            case "CORRECTIVE" -> correctiveCost;
            default -> BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        };
    }

    /**
     * Check if all parts are available
     */
    private boolean checkPartsAvailable(List<MaintenancePart> parts) {
        if (parts == null || parts.isEmpty()) {
            return true;
        }
        return parts.stream().allMatch(part -> 
            part.getStockQuantity() != null && part.getStockQuantity() > 0
        );
    }

    /**
     * Get names of missing parts
     */
    private List<String> getMissingPartNames(List<MaintenancePart> parts) {
        if (parts == null || parts.isEmpty()) {
            return List.of();
        }
        return parts.stream()
                .filter(part -> part.getStockQuantity() == null || part.getStockQuantity() == 0)
                .map(MaintenancePart::getName)
                .toList();
    }

    /**
     * Create default maintenance action for estimation
     */
    private MaintenanceAction createDefaultAction(Machine machine, List<MaintenancePart> parts) {
        return MaintenanceAction.builder()
                .machine(machine)
                .type(MaintenanceActionType.PREVENTIVE)
                .estimatedDurationHours(4.0)
                .laborCostPerHour(BigDecimal.valueOf(120.0))
                .parts(parts == null ? Set.of() : Set.copyOf(parts))
                .status(MaintenanceActionStatus.PLANNED)
                .scheduledDate(java.time.LocalDateTime.now())
                .build();
    }
}
