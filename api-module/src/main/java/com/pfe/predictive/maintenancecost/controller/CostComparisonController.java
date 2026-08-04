package com.pfe.predictive.maintenancecost.controller;

import com.pfe.predictive.maintenancecost.dto.CostComparisonRequest;
import com.pfe.predictive.maintenancecost.dto.CostComparisonResponse;
import com.pfe.predictive.maintenancecost.entity.Machine;
import com.pfe.predictive.maintenancecost.entity.MaintenanceAction;
import com.pfe.predictive.maintenancecost.repository.MachineRepository;
import com.pfe.predictive.maintenancecost.repository.MaintenanceActionRepository;
import com.pfe.predictive.maintenancecost.service.CostComparisonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Cost Comparison Controller
 * Provides endpoints for comparing preventive vs corrective maintenance costs
 * 
 * @author Finance Module
 * @version 1.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/maintenance-cost/costs")
@RequiredArgsConstructor
@Tag(name = "Cost Comparison", description = "Compare preventive vs corrective maintenance costs")
public class CostComparisonController {

    private final CostComparisonService costComparisonService;
    private final MachineRepository machineRepository;
    private final MaintenanceActionRepository maintenanceActionRepository;

    /**
     * Compare costs for a specific maintenance action
     * 
     * POST /api/v1/finance/costs/compare
     * 
     * @param request Cost comparison request
     * @return Cost comparison report
     */
    @PostMapping("/compare")
    @PreAuthorize("hasAnyRole('FINANCE_MANAGER', 'MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Compare maintenance costs", 
               description = "Compare preventive vs corrective maintenance costs for a specific action")
    public ResponseEntity<CostComparisonResponse> compareCosts(@Valid @RequestBody CostComparisonRequest request) {
        log.info("Cost comparison request - Machine: {}, Action: {}", 
                 request.getMachineId(), request.getActionId());

        Machine machine = machineRepository.findById(request.getMachineId())
                .orElseThrow(() -> new EntityNotFoundException("Machine not found: " + request.getMachineId()));

        MaintenanceAction action = maintenanceActionRepository.findById(request.getActionId())
                .orElseThrow(() -> new EntityNotFoundException("Action not found: " + request.getActionId()));

        CostComparisonResponse response = costComparisonService.buildCostComparisonReport(
                machine,
                action,
                request.getEstimatedFailureDowntimeHours()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Get cost comparison for a machine's next planned action
     * 
     * GET /api/v1/finance/costs/compare/machine/{machineId}
     * 
     * @param machineId Machine ID
     * @param estimatedDowntimeHours Estimated downtime if failure occurs (default: 8.0)
     * @return Cost comparison report
     */
    @GetMapping("/compare/machine/{machineId}")
    @PreAuthorize("hasAnyRole('FINANCE_MANAGER', 'MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Get cost comparison for machine", 
               description = "Get cost comparison for a machine's next planned maintenance action")
    public ResponseEntity<CostComparisonResponse> compareCostsForMachine(
            @PathVariable Long machineId,
            @RequestParam(defaultValue = "8.0") Double estimatedDowntimeHours) {
        
        log.info("Cost comparison for machine: {}", machineId);

        Machine machine = machineRepository.findById(machineId)
                .orElseThrow(() -> new EntityNotFoundException("Machine not found: " + machineId));

        MaintenanceAction action = maintenanceActionRepository
                .findByMachineOrderByScheduledDateDesc(machine)
                .stream()
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("No maintenance action found for machine: " + machineId));

        CostComparisonResponse response = costComparisonService.buildCostComparisonReport(
                machine,
                action,
                estimatedDowntimeHours
        );

        return ResponseEntity.ok(response);
    }
}
