package com.pfe.predictive.maintenancecost.service;

import com.pfe.predictive.maintenancecost.entity.FailureEvent;
import com.pfe.predictive.maintenancecost.entity.Machine;
import com.pfe.predictive.maintenancecost.entity.MaintenanceAction;
import com.pfe.predictive.maintenancecost.entity.MaintenancePart;
import com.pfe.predictive.maintenancecost.enums.MaintenanceActionStatus;
import com.pfe.predictive.maintenancecost.repository.FailureEventRepository;
import com.pfe.predictive.maintenancecost.repository.MachineRepository;
import com.pfe.predictive.maintenancecost.repository.MaintenanceActionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Maintenance Cost Service - Orchestrates financial tracking
 * 
 * THIS IS THE CORE OF THE FINANCIAL ENGINE 🔥
 * 
 * Responsibilities:
 * 1. Calculate maintenance action costs (labor + parts)
 * 2. Update budget when maintenance is completed
 * 3. Update budget when failure occurs
 * 4. Provide cost analytics
 * 
 * @author Finance Module
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MaintenanceCostService {

    private final MaintenanceActionRepository actionRepository;
    private final FailureEventRepository failureRepository;
    private final MachineRepository machineRepository;
    private final BudgetService budgetService;

    /**
     * Handle maintenance action completion
     * CRITICAL: This updates the budget automatically
     * 
     * @param actionId Maintenance action ID
     */
    public void handleMaintenanceCompleted(Long actionId) {
        log.info("🔧 Processing completed maintenance action: {}", actionId);

        MaintenanceAction action = actionRepository.findById(actionId)
            .orElseThrow(() -> new IllegalArgumentException("Maintenance action not found: " + actionId));

        if (action.getStatus() != MaintenanceActionStatus.COMPLETED) {
            throw new IllegalStateException("Action must be COMPLETED to process costs");
        }

        // Calculate total cost
        BigDecimal totalCost = calculateMaintenanceActionCost(action);
        
        log.info("💰 Maintenance cost calculated - Action: {}, Total: {}", actionId, totalCost);

        // Determine department and period from machine location
        Machine machine = action.getMachine();
        String department = determineDepartment(machine);
        String period = determineCurrentPeriod();

        // Update budget
        try {
            budgetService.addCostToBudget(department, period, totalCost);
            log.info("✅ Budget updated successfully - Department: {}, Period: {}", department, period);
        } catch (Exception e) {
            log.error("❌ Failed to update budget: {}", e.getMessage());
            throw new RuntimeException("Budget update failed: " + e.getMessage(), e);
        }
    }

    /**
     * Handle failure event
     * CRITICAL: This updates the budget with downtime costs
     * 
     * @param failureId Failure event ID
     */
    public void handleFailureEvent(Long failureId) {
        log.info("🚨 Processing failure event: {}", failureId);

        FailureEvent event = failureRepository.findById(failureId)
            .orElseThrow(() -> new IllegalArgumentException("Failure event not found: " + failureId));

        // Cost is already computed by @PrePersist in FailureEvent entity
        BigDecimal cost = event.getTotalCostIncurred();
        
        log.warn("💸 Failure cost incurred - Event: {}, Cost: {}, Downtime: {}h", 
                 failureId, cost, event.getActualDowntimeHours());

        // Determine department and period
        Machine machine = event.getMachine();
        String department = determineDepartment(machine);
        String period = determineCurrentPeriod();

        // Update budget
        try {
            budgetService.addCostToBudget(department, period, cost);
            log.info("✅ Budget updated with failure cost - Department: {}, Period: {}", department, period);
        } catch (Exception e) {
            log.error("❌ Failed to update budget with failure cost: {}", e.getMessage());
            throw new RuntimeException("Budget update failed: " + e.getMessage(), e);
        }
    }

    /**
     * Calculate total cost of maintenance action
     * Formula: Labor Cost + Parts Cost
     * 
     * @param action Maintenance action
     * @return Total cost
     */
    public BigDecimal calculateMaintenanceActionCost(MaintenanceAction action) {
        // Labor cost = hours × hourly rate
        BigDecimal laborCost = action.getLaborCostPerHour()
            .multiply(BigDecimal.valueOf(action.getEstimatedDurationHours()))
            .setScale(4, RoundingMode.HALF_UP);

        // Parts cost = sum of all part unit costs
        BigDecimal partsCost = action.getParts().stream()
            .map(MaintenancePart::getUnitCost)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(4, RoundingMode.HALF_UP);

        BigDecimal totalCost = laborCost.add(partsCost);

        log.debug("Cost breakdown - Labor: {}, Parts: {}, Total: {}", 
                  laborCost, partsCost, totalCost);

        return totalCost;
    }

    /**
     * Calculate estimated cost of failure based on predicted downtime
     * Used by ML prediction system
     * 
     * @param machineId Machine ID
     * @param predictedDowntimeHours Predicted downtime
     * @return Estimated cost
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateEstimatedFailureCost(Long machineId, double predictedDowntimeHours) {
        Machine machine = machineRepository.findById(machineId)
            .orElseThrow(() -> new IllegalArgumentException("Machine not found: " + machineId));

        BigDecimal cost = machine.getHourlyProductionValue()
            .multiply(BigDecimal.valueOf(predictedDowntimeHours))
            .setScale(4, RoundingMode.HALF_UP);

        log.debug("Estimated failure cost - Machine: {}, Downtime: {}h, Cost: {}", 
                  machineId, predictedDowntimeHours, cost);

        return cost;
    }

    /**
     * Decision engine: Should we do preventive maintenance?
     * Logic: If estimated failure cost > preventive maintenance cost → YES
     * 
     * @param machineId Machine ID
     * @param predictedDowntimeHours Predicted downtime if failure occurs
     * @param preventiveMaintenanceCost Cost of preventive maintenance
     * @return true if preventive maintenance is recommended
     */
    @Transactional(readOnly = true)
    public boolean shouldDoPreventiveMaintenance(Long machineId, 
                                                  double predictedDowntimeHours, 
                                                  BigDecimal preventiveMaintenanceCost) {
        
        BigDecimal estimatedFailureCost = calculateEstimatedFailureCost(machineId, predictedDowntimeHours);
        
        boolean recommend = estimatedFailureCost.compareTo(preventiveMaintenanceCost) > 0;
        
        BigDecimal savings = estimatedFailureCost.subtract(preventiveMaintenanceCost);
        
        log.info("🧠 Preventive maintenance decision - Machine: {}, " +
                 "Failure cost: {}, Preventive cost: {}, Savings: {}, Recommend: {}", 
                 machineId, estimatedFailureCost, preventiveMaintenanceCost, savings, recommend);

        return recommend;
    }

    /**
     * Get total maintenance costs for a machine
     * @param machineId Machine ID
     * @return Total costs
     */
    @Transactional(readOnly = true)
    public BigDecimal getTotalMaintenanceCosts(Long machineId) {
        Machine machine = machineRepository.findById(machineId)
            .orElseThrow(() -> new IllegalArgumentException("Machine not found: " + machineId));

        return actionRepository.findByMachineOrderByScheduledDateDesc(machine)
            .stream()
            .filter(action -> action.getStatus() == MaintenanceActionStatus.COMPLETED)
            .map(this::calculateMaintenanceActionCost)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Get total failure costs for a machine
     * @param machineId Machine ID
     * @return Total failure costs
     */
    @Transactional(readOnly = true)
    public BigDecimal getTotalFailureCosts(Long machineId) {
        Machine machine = machineRepository.findById(machineId)
            .orElseThrow(() -> new IllegalArgumentException("Machine not found: " + machineId));

        return failureRepository.findByMachineOrderByOccurredAtDesc(machine)
            .stream()
            .map(FailureEvent::getTotalCostIncurred)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Get total costs (maintenance + failures) for a machine
     * @param machineId Machine ID
     * @return Total costs
     */
    @Transactional(readOnly = true)
    public BigDecimal getTotalCosts(Long machineId) {
        BigDecimal maintenanceCosts = getTotalMaintenanceCosts(machineId);
        BigDecimal failureCosts = getTotalFailureCosts(machineId);
        return maintenanceCosts.add(failureCosts);
    }

    // ==================== HELPER METHODS ====================

    /**
     * Determine department from machine location
     * Simple logic: extract first word from location
     * Override this method for custom logic
     */
    private String determineDepartment(Machine machine) {
        String location = machine.getLocation();
        if (location == null || location.isBlank()) {
            return "General";
        }
        // Extract first word (e.g., "Production Floor A" → "Production")
        String[] parts = location.split("\\s+");
        return parts[0];
    }

    /**
     * Determine current period
     * Format: YYYY-QX (e.g., "2026-Q2")
     */
    private String determineCurrentPeriod() {
        LocalDateTime now = LocalDateTime.now();
        int year = now.getYear();
        int month = now.getMonthValue();
        int quarter = ((month - 1) / 3) + 1;
        return String.format("%d-Q%d", year, quarter);
    }
}
