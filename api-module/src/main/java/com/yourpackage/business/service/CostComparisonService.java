package com.yourpackage.business.service;

import com.pfe.predictive.maintenancecost.enums.MaintenanceActionStatus;
import com.pfe.predictive.maintenancecost.entity.Machine;
import com.pfe.predictive.maintenancecost.entity.MaintenanceAction;
import com.pfe.predictive.maintenancecost.repository.MachineRepository;
import com.pfe.predictive.maintenancecost.repository.MaintenanceActionRepository;
import com.yourpackage.business.dto.CompareRequestDTO;
import com.yourpackage.business.dto.CostComparisonDTO;
import com.yourpackage.business.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Comparator;

@Service
public class CostComparisonService {

    private final CostCalculationService costCalculationService;
    private final MachineRepository machineRepository;
    private final MaintenanceActionRepository maintenanceActionRepository;

    public CostComparisonService(CostCalculationService costCalculationService,
                                 MachineRepository machineRepository,
                                 MaintenanceActionRepository maintenanceActionRepository) {
        this.costCalculationService = costCalculationService;
        this.machineRepository = machineRepository;
        this.maintenanceActionRepository = maintenanceActionRepository;
    }

    public CostComparisonDTO compare(CompareRequestDTO compareRequest) {
        Machine machine = machineRepository.findById(compareRequest.getMachineId())
                .orElseThrow(() -> new ResourceNotFoundException("Machine not found with id: " + compareRequest.getMachineId()));

        MaintenanceAction action = maintenanceActionRepository.findById(compareRequest.getActionId())
                .orElseThrow(() -> new ResourceNotFoundException("Maintenance action not found with id: " + compareRequest.getActionId()));

        if (action.getMachine() == null || action.getMachine().getId() == null || !action.getMachine().getId().equals(machine.getId())) {
            throw new IllegalArgumentException("The provided actionId does not belong to the provided machineId");
        }

        return costCalculationService.buildCostComparisonReport(
                machine,
                action,
                compareRequest.getEstimatedFailureDowntimeHours()
        );
    }

    public CostComparisonDTO compareLatestForMachine(Long machineId, Double estimatedFailureDowntimeHours) {
        Machine machine = machineRepository.findById(machineId)
                .orElseThrow(() -> new ResourceNotFoundException("Machine not found with id: " + machineId));

        MaintenanceAction action = maintenanceActionRepository.findByMachineOrderByScheduledDateDesc(machine)
                .stream()
                .filter(a -> a.getStatus() == MaintenanceActionStatus.PLANNED || a.getStatus() == MaintenanceActionStatus.IN_PROGRESS)
                .max(Comparator.comparing(MaintenanceAction::getScheduledDate))
                .orElseThrow(() -> new ResourceNotFoundException("No maintenance action found for machine id: " + machineId));

        double safeDowntime = estimatedFailureDowntimeHours == null || estimatedFailureDowntimeHours <= 0
                ? 8.0
                : estimatedFailureDowntimeHours;

        return costCalculationService.buildCostComparisonReport(machine, action, safeDowntime);
    }
}
