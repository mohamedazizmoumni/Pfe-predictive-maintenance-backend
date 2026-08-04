package com.pfe.predictive.config;

import com.pfe.predictive.maintenancecost.entity.Machine;
import com.pfe.predictive.maintenancecost.entity.MaintenanceAction;
import com.pfe.predictive.maintenancecost.entity.MaintenanceBudget;
import com.pfe.predictive.maintenancecost.entity.MaintenancePart;
import com.pfe.predictive.maintenancecost.enums.CriticalityLevel;
import com.pfe.predictive.maintenancecost.enums.MachineStatus;
import com.pfe.predictive.maintenancecost.enums.MaintenanceActionStatus;
import com.pfe.predictive.maintenancecost.enums.MaintenanceActionType;
import com.pfe.predictive.maintenancecost.repository.MachineRepository;
import com.pfe.predictive.maintenancecost.repository.MaintenanceActionRepository;
import com.pfe.predictive.maintenancecost.repository.MaintenanceBudgetRepository;
import com.pfe.predictive.maintenancecost.repository.MaintenancePartRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * Financial Data Initializer
 * Seeds sample data for testing the financial system
 *
 * Creates:
 * - 3 sample machines with different criticality levels
 * - 5 maintenance parts with varying stock levels
 * - 2 maintenance budgets for different departments
 * - 2 maintenance actions (preventive and corrective)
 *
 * Gated behind SEED_DEMO_DATA (default off) - a fresh production database
 * should stay empty, not fill up with sample finance data on first boot.
 *
 * @author Finance Module
 * @version 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FinancialDataInitializer implements CommandLineRunner {

    private final MachineRepository machineRepository;
    private final MaintenancePartRepository maintenancePartRepository;
    private final MaintenanceBudgetRepository maintenanceBudgetRepository;
    private final MaintenanceActionRepository maintenanceActionRepository;

    @Value("${app.demo-data.enabled:false}")
    private boolean demoDataEnabled;

    @Override
    public void run(String... args) {
        if (!demoDataEnabled) {
            log.info("Demo data seeding disabled (set SEED_DEMO_DATA=true to enable) - skipping financial sample data");
            return;
        }

        // Skip if data already exists
        if (machineRepository.count() > 0) {
            log.info("Financial data already initialized - skipping");
            return;
        }

        log.info("Initializing financial sample data...");

        // Create machines
        Machine cncMillAlpha = machineRepository.save(Machine.builder()
                .name("CNC Mill Alpha")
                .location("Workshop A")
                .status(MachineStatus.RUNNING)
                .hourlyProductionValue(new BigDecimal("200.00"))
                .replacementCost(new BigDecimal("45000.00"))
                .criticalityLevel(CriticalityLevel.HIGH)
                .age(36)
                .build());
        log.info("Created machine: {}", cncMillAlpha.getName());

        Machine hydraulicPressB = machineRepository.save(Machine.builder()
                .name("Hydraulic Press B")
                .location("Workshop B")
                .status(MachineStatus.UNDER_MAINTENANCE)
                .hourlyProductionValue(new BigDecimal("150.00"))
                .replacementCost(new BigDecimal("30000.00"))
                .criticalityLevel(CriticalityLevel.MEDIUM)
                .age(60)
                .build());
        log.info("Created machine: {}", hydraulicPressB.getName());

        Machine conveyorBeltC = machineRepository.save(Machine.builder()
                .name("Conveyor Belt C")
                .location("Assembly")
                .status(MachineStatus.RUNNING)
                .hourlyProductionValue(new BigDecimal("80.00"))
                .replacementCost(new BigDecimal("12000.00"))
                .criticalityLevel(CriticalityLevel.CRITICAL)
                .age(84)
                .build());
        log.info("Created machine: {}", conveyorBeltC.getName());

        // Create maintenance parts
        MaintenancePart bearingKit = maintenancePartRepository.save(MaintenancePart.builder()
                .name("Bearing Kit")
                .referenceCode("BRG-001")
                .unitCost(new BigDecimal("120.00"))
                .stockQuantity(5)
                .leadTimeDays(2)
                .build());
        log.info("Created part: {}", bearingKit.getName());

        MaintenancePart hydraulicSeal = maintenancePartRepository.save(MaintenancePart.builder()
                .name("Hydraulic Seal")
                .referenceCode("HYD-220")
                .unitCost(new BigDecimal("85.00"))
                .stockQuantity(0)  // Out of stock
                .leadTimeDays(7)
                .build());
        log.info("Created part: {} (OUT OF STOCK)", hydraulicSeal.getName());

        MaintenancePart driveBelt = maintenancePartRepository.save(MaintenancePart.builder()
                .name("Drive Belt")
                .referenceCode("BLT-440")
                .unitCost(new BigDecimal("45.00"))
                .stockQuantity(12)
                .leadTimeDays(1)
                .build());
        log.info("Created part: {}", driveBelt.getName());

        MaintenancePart controlBoard = maintenancePartRepository.save(MaintenancePart.builder()
                .name("Control Board")
                .referenceCode("PCB-110")
                .unitCost(new BigDecimal("380.00"))
                .stockQuantity(1)
                .leadTimeDays(14)
                .build());
        log.info("Created part: {}", controlBoard.getName());

        MaintenancePart pressureValve = maintenancePartRepository.save(MaintenancePart.builder()
                .name("Pressure Valve")
                .referenceCode("VLV-330")
                .unitCost(new BigDecimal("210.00"))
                .stockQuantity(3)
                .leadTimeDays(3)
                .build());
        log.info("Created part: {}", pressureValve.getName());

        // Create maintenance budgets
        MaintenanceBudget budgetA = maintenanceBudgetRepository.save(MaintenanceBudget.builder()
                .department("Workshop A")
                .period("2026-Q2")
                .allocatedAmount(new BigDecimal("15000.00"))
                .spentAmount(new BigDecimal("9800.00"))
                .build());
        log.info("Created budget: {} - {} (Allocated: €{}, Spent: €{})", 
                 budgetA.getDepartment(), budgetA.getPeriod(), 
                 budgetA.getAllocatedAmount(), budgetA.getSpentAmount());

        MaintenanceBudget budgetB = maintenanceBudgetRepository.save(MaintenanceBudget.builder()
                .department("Workshop B")
                .period("2026-Q2")
                .allocatedAmount(new BigDecimal("10000.00"))
                .spentAmount(new BigDecimal("3200.00"))
                .build());
        log.info("Created budget: {} - {} (Allocated: €{}, Spent: €{})", 
                 budgetB.getDepartment(), budgetB.getPeriod(), 
                 budgetB.getAllocatedAmount(), budgetB.getSpentAmount());

        // Create maintenance actions
        MaintenanceAction preventiveAction = maintenanceActionRepository.save(MaintenanceAction.builder()
                .machine(cncMillAlpha)
                .type(MaintenanceActionType.PREVENTIVE)
                .estimatedDurationHours(4.0)
                .laborCostPerHour(new BigDecimal("120.00"))
                .status(MaintenanceActionStatus.PLANNED)
                .scheduledDate(LocalDateTime.now().plusDays(3))
                .parts(Set.of(bearingKit, driveBelt))
                .build());
        log.info("Created preventive action for: {} (Scheduled: {})", 
                 cncMillAlpha.getName(), preventiveAction.getScheduledDate());

        MaintenanceAction correctiveAction = maintenanceActionRepository.save(MaintenanceAction.builder()
                .machine(hydraulicPressB)
                .type(MaintenanceActionType.CORRECTIVE)
                .estimatedDurationHours(8.0)
                .laborCostPerHour(new BigDecimal("150.00"))
                .status(MaintenanceActionStatus.IN_PROGRESS)
                .scheduledDate(LocalDateTime.now())
                .parts(Set.of(hydraulicSeal, pressureValve))
                .build());
        log.info("Created corrective action for: {} (Status: IN_PROGRESS)", 
                 hydraulicPressB.getName());

        log.info("Financial sample data initialization completed successfully!");
        log.info("Summary: 3 machines, 5 parts, 2 budgets, 2 actions");
    }
}
