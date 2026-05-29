package com.pfe.predictive.maintenancecost.service;

import com.pfe.predictive.maintenancecost.dto.CostComparisonResponse;
import com.pfe.predictive.maintenancecost.entity.Machine;
import com.pfe.predictive.maintenancecost.entity.MaintenanceAction;
import com.pfe.predictive.maintenancecost.entity.MaintenancePart;
import com.pfe.predictive.maintenancecost.enums.CriticalityLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Cost Comparison Service
 * Compares preventive maintenance cost vs corrective (failure) cost
 * Helps make data-driven maintenance decisions
 * 
 * @author Finance Module
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CostComparisonService {

    private static final BigDecimal EMERGENCY_LABOR_RATE = BigDecimal.valueOf(150.0);
    private static final BigDecimal EMERGENCY_LABOR_MARKUP = BigDecimal.valueOf(1.5);
    private static final BigDecimal EMERGENCY_PARTS_MARKUP = BigDecimal.valueOf(1.3);
    private static final BigDecimal SIGNIFICANT_SAVINGS_THRESHOLD = BigDecimal.valueOf(500);

    /**
     * Calculate preventive maintenance cost
     * Formula: Labor Cost + Parts Cost
     * 
     * @param action Planned maintenance action
     * @return Total preventive cost
     */
    public BigDecimal calculatePreventiveCost(MaintenanceAction action) {
        if (action == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        // Parts cost
        BigDecimal partsCost = action.getParts() == null
                ? BigDecimal.ZERO
                : action.getParts().stream()
                .map(MaintenancePart::getUnitCost)
                .filter(cost -> cost != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Labor cost
        double durationHours = action.getEstimatedDurationHours() == null ? 0.0 : action.getEstimatedDurationHours();
        BigDecimal laborRate = action.getLaborCostPerHour() == null ? BigDecimal.ZERO : action.getLaborCostPerHour();
        BigDecimal laborCost = BigDecimal.valueOf(durationHours).multiply(laborRate);

        BigDecimal total = partsCost.add(laborCost).setScale(2, RoundingMode.HALF_UP);
        
        log.debug("Preventive cost - Labor: {}, Parts: {}, Total: {}", 
                  laborCost, partsCost, total);

        return total;
    }

    /**
     * Calculate corrective (failure) cost
     * Formula: Production Loss + Emergency Labor + Emergency Parts
     * 
     * Emergency costs are higher due to:
     * - Overtime labor rates (1.5x markup)
     * - Rush parts delivery (1.3x markup)
     * - Production downtime loss
     * 
     * @param machine Machine that failed
     * @param estimatedDowntimeHours Expected downtime
     * @param parts Parts needed for repair
     * @return Total corrective cost
     */
    public BigDecimal calculateCorrectiveCost(Machine machine, double estimatedDowntimeHours, List<MaintenancePart> parts) {
        if (machine == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal downtime = BigDecimal.valueOf(estimatedDowntimeHours);

        // Production loss = downtime × hourly production value
        BigDecimal hourlyProductionValue = machine.getHourlyProductionValue() == null
                ? BigDecimal.ZERO
                : machine.getHourlyProductionValue();
        BigDecimal productionLoss = downtime.multiply(hourlyProductionValue);

        // Emergency labor = downtime × emergency rate × markup
        BigDecimal emergencyLabor = downtime
            .multiply(EMERGENCY_LABOR_RATE)
            .multiply(EMERGENCY_LABOR_MARKUP);

        // Emergency parts = parts cost × markup
        BigDecimal emergencyParts = parts == null
                ? BigDecimal.ZERO
                : parts.stream()
                .map(MaintenancePart::getUnitCost)
                .filter(cost -> cost != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .multiply(EMERGENCY_PARTS_MARKUP);

        BigDecimal total = productionLoss.add(emergencyLabor).add(emergencyParts)
            .setScale(2, RoundingMode.HALF_UP);

        log.debug("Corrective cost - Production loss: {}, Emergency labor: {}, Emergency parts: {}, Total: {}", 
                  productionLoss, emergencyLabor, emergencyParts, total);

        return total;
    }

    /**
     * Calculate savings from preventive maintenance
     * @param correctiveCost Cost if failure occurs
     * @param preventiveCost Cost of preventive action
     * @return Savings (always >= 0)
     */
    public BigDecimal calculateSavings(BigDecimal correctiveCost, BigDecimal preventiveCost) {
        BigDecimal savings = correctiveCost.subtract(preventiveCost);
        return savings.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Build comprehensive cost comparison report
     * 
     * @param machine Machine to analyze
     * @param action Planned maintenance action
     * @param estimatedFailureDowntimeHours Expected downtime if failure occurs
     * @return Detailed comparison report
     */
    public CostComparisonResponse buildCostComparisonReport(Machine machine,
                                                            MaintenanceAction action,
                                                            double estimatedFailureDowntimeHours) {
        log.info("Building cost comparison - Machine: {}, Action: {}", 
                 machine.getId(), action.getId());

        BigDecimal preventiveCost = calculatePreventiveCost(action);
        BigDecimal correctiveCost = calculateCorrectiveCost(
            machine, 
            estimatedFailureDowntimeHours,
            action.getParts() == null ? List.of() : action.getParts().stream().toList()
        );
        BigDecimal savings = calculateSavings(correctiveCost, preventiveCost);

        // Generate recommendation
        String recommendation;
        if (savings.compareTo(SIGNIFICANT_SAVINGS_THRESHOLD) > 0) {
            recommendation = "Strongly recommended: preventive action saves significant cost";
        } else if (savings.compareTo(BigDecimal.ZERO) > 0) {
            recommendation = "Preventive action is more cost-effective";
        } else {
            recommendation = "Costs are comparable - monitor closely";
        }

        String urgencyLevel = mapUrgency(machine.getCriticalityLevel());

        log.info("Cost comparison result - Preventive: {}, Corrective: {}, Savings: {}, Recommendation: {}", 
                 preventiveCost, correctiveCost, savings, recommendation);

        return CostComparisonResponse.builder()
                .machineId(machine.getId())
                .machineName(machine.getName())
                .preventiveCost(preventiveCost)
                .correctiveCost(correctiveCost)
                .estimatedSavings(savings)
                .recommendation(recommendation)
                .urgencyLevel(urgencyLevel)
                .estimatedDowntimeHours(estimatedFailureDowntimeHours)
                .build();
    }

    /**
     * Map machine criticality to urgency level
     */
    private String mapUrgency(CriticalityLevel criticalityLevel) {
        if (criticalityLevel == null) {
            return "LOW";
        }
        return switch (criticalityLevel) {
            case CRITICAL -> "CRITICAL";
            case HIGH -> "HIGH";
            case MEDIUM -> "MEDIUM";
            case LOW -> "LOW";
        };
    }
}
