package com.yourpackage.business.service;

import com.pfe.predictive.maintenancecost.entity.Machine;
import com.pfe.predictive.maintenancecost.entity.MaintenanceAction;
import com.pfe.predictive.maintenancecost.entity.MaintenancePart;
import com.pfe.predictive.maintenancecost.enums.CriticalityLevel;
import com.yourpackage.business.dto.CostComparisonDTO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class CostCalculationService {

    private static final BigDecimal EMERGENCY_LABOR_RATE = BigDecimal.valueOf(150.0);
    private static final BigDecimal EMERGENCY_LABOR_MARKUP = BigDecimal.valueOf(1.5);
    private static final BigDecimal EMERGENCY_PARTS_MARKUP = BigDecimal.valueOf(1.3);
    private static final BigDecimal SIGNIFICANT_SAVINGS_THRESHOLD = BigDecimal.valueOf(500);

    public BigDecimal calculatePreventiveCost(MaintenanceAction action) {
        if (action == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal partsCost = action.getParts() == null
                ? BigDecimal.ZERO
                : action.getParts().stream()
                .map(MaintenancePart::getUnitCost)
                .filter(cost -> cost != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        double durationHours = action.getEstimatedDurationHours() == null ? 0.0 : action.getEstimatedDurationHours();
        BigDecimal laborRate = action.getLaborCostPerHour() == null ? BigDecimal.ZERO : action.getLaborCostPerHour();

        BigDecimal laborCost = BigDecimal.valueOf(durationHours)
            .multiply(laborRate);

        return partsCost.add(laborCost).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateCorrectiveCost(Machine machine, double estimatedDowntimeHours, List<MaintenancePart> parts) {
        if (machine == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal downtime = BigDecimal.valueOf(estimatedDowntimeHours);

        BigDecimal hourlyProductionValue = machine.getHourlyProductionValue() == null
                ? BigDecimal.ZERO
                : machine.getHourlyProductionValue();

        BigDecimal productionLoss = downtime.multiply(hourlyProductionValue);
        BigDecimal emergencyLabor = downtime.multiply(EMERGENCY_LABOR_RATE).multiply(EMERGENCY_LABOR_MARKUP);

        BigDecimal emergencyParts = parts == null
                ? BigDecimal.ZERO
                : parts.stream()
                .map(MaintenancePart::getUnitCost)
                .filter(cost -> cost != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .multiply(EMERGENCY_PARTS_MARKUP);

        return productionLoss.add(emergencyLabor).add(emergencyParts).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateSavings(BigDecimal correctiveCost, BigDecimal preventiveCost) {
        BigDecimal savings = correctiveCost.subtract(preventiveCost);
        return savings.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    public CostComparisonDTO buildCostComparisonReport(Machine machine,
                                                       MaintenanceAction action,
                                                       double estimatedFailureDowntimeHours) {
        BigDecimal preventiveCost = calculatePreventiveCost(action);
        BigDecimal correctiveCost = calculateCorrectiveCost(machine, estimatedFailureDowntimeHours,
                action.getParts() == null ? List.of() : action.getParts().stream().toList());
        BigDecimal savings = calculateSavings(correctiveCost, preventiveCost);

        String recommendation;
        if (savings.compareTo(SIGNIFICANT_SAVINGS_THRESHOLD) > 0) {
            recommendation = "Strongly recommended: preventive action saves significant cost";
        } else if (savings.compareTo(BigDecimal.ZERO) > 0) {
            recommendation = "Preventive action is more cost-effective";
        } else {
            recommendation = "Costs are comparable - monitor closely";
        }

        String urgencyLevel = mapUrgency(machine.getCriticalityLevel());

        return CostComparisonDTO.builder()
                .machineId(machine.getId())
                .machineName(machine.getName())
                .preventiveCost(preventiveCost)
                .correctiveCost(correctiveCost)
                .estimatedSavings(savings)
                .recommendation(recommendation)
                .urgencyLevel(urgencyLevel)
                .build();
    }

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
