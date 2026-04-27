package com.yourpackage.business.config;

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
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {

    private final MachineRepository machineRepository;
    private final MaintenancePartRepository maintenancePartRepository;
    private final MaintenanceBudgetRepository maintenanceBudgetRepository;
    private final MaintenanceActionRepository maintenanceActionRepository;

    public DataInitializer(MachineRepository machineRepository,
                           MaintenancePartRepository maintenancePartRepository,
                           MaintenanceBudgetRepository maintenanceBudgetRepository,
                           MaintenanceActionRepository maintenanceActionRepository) {
        this.machineRepository = machineRepository;
        this.maintenancePartRepository = maintenancePartRepository;
        this.maintenanceBudgetRepository = maintenanceBudgetRepository;
        this.maintenanceActionRepository = maintenanceActionRepository;
    }

    @Override
    public void run(String... args) {
        if (machineRepository.count() > 0) {
            return;
        }

        Machine cncMillAlpha = machineRepository.save(Machine.builder()
                .name("CNC Mill Alpha")
                .location("Workshop A")
                .status(MachineStatus.RUNNING)
                .hourlyProductionValue(new BigDecimal("200.00"))
                .replacementCost(new BigDecimal("45000.00"))
                .criticalityLevel(CriticalityLevel.HIGH)
                .age(36)
                .build());

        Machine hydraulicPressB = machineRepository.save(Machine.builder()
                .name("Hydraulic Press B")
                .location("Workshop B")
                .status(MachineStatus.UNDER_MAINTENANCE)
                .hourlyProductionValue(new BigDecimal("150.00"))
                .replacementCost(new BigDecimal("30000.00"))
                .criticalityLevel(CriticalityLevel.MEDIUM)
                .age(60)
                .build());

        machineRepository.save(Machine.builder()
                .name("Conveyor Belt C")
                .location("Assembly")
                .status(MachineStatus.RUNNING)
                .hourlyProductionValue(new BigDecimal("80.00"))
                .replacementCost(new BigDecimal("12000.00"))
                .criticalityLevel(CriticalityLevel.CRITICAL)
                .age(84)
                .build());

        MaintenancePart bearingKit = maintenancePartRepository.save(MaintenancePart.builder()
                .name("Bearing Kit")
                .referenceCode("BRG-001")
                .unitCost(new BigDecimal("120.00"))
                .stockQuantity(5)
                .leadTimeDays(2)
                .build());

        MaintenancePart hydraulicSeal = maintenancePartRepository.save(MaintenancePart.builder()
                .name("Hydraulic Seal")
                .referenceCode("HYD-220")
                .unitCost(new BigDecimal("85.00"))
                .stockQuantity(0)
                .leadTimeDays(7)
                .build());

        MaintenancePart driveBelt = maintenancePartRepository.save(MaintenancePart.builder()
                .name("Drive Belt")
                .referenceCode("BLT-440")
                .unitCost(new BigDecimal("45.00"))
                .stockQuantity(12)
                .leadTimeDays(1)
                .build());

        maintenancePartRepository.save(MaintenancePart.builder()
                .name("Control Board")
                .referenceCode("PCB-110")
                .unitCost(new BigDecimal("380.00"))
                .stockQuantity(1)
                .leadTimeDays(14)
                .build());

        MaintenancePart pressureValve = maintenancePartRepository.save(MaintenancePart.builder()
                .name("Pressure Valve")
                .referenceCode("VLV-330")
                .unitCost(new BigDecimal("210.00"))
                .stockQuantity(3)
                .leadTimeDays(3)
                .build());

        maintenanceBudgetRepository.save(MaintenanceBudget.builder()
                .department("Workshop A")
                .period("2025-Q2")
                .allocatedAmount(new BigDecimal("15000.00"))
                .spentAmount(new BigDecimal("9800.00"))
                .build());

        maintenanceBudgetRepository.save(MaintenanceBudget.builder()
                .department("Workshop B")
                .period("2025-Q2")
                .allocatedAmount(new BigDecimal("10000.00"))
                .spentAmount(new BigDecimal("3200.00"))
                .build());

        maintenanceActionRepository.save(MaintenanceAction.builder()
                .machine(cncMillAlpha)
                .type(MaintenanceActionType.PREVENTIVE)
                .estimatedDurationHours(4.0)
                .laborCostPerHour(new BigDecimal("120.00"))
                .status(MaintenanceActionStatus.PLANNED)
                .scheduledDate(LocalDateTime.now().plusDays(3))
                .parts(Set.of(bearingKit, driveBelt))
                .build());

        maintenanceActionRepository.save(MaintenanceAction.builder()
                .machine(hydraulicPressB)
                .type(MaintenanceActionType.CORRECTIVE)
                .estimatedDurationHours(8.0)
                .laborCostPerHour(new BigDecimal("150.00"))
                .status(MaintenanceActionStatus.IN_PROGRESS)
                .scheduledDate(LocalDateTime.now())
                .parts(Set.of(hydraulicSeal, pressureValve))
                .build());
    }
}