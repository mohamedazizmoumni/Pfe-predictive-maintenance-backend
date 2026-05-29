package com.pfe.predictive.maintenancecost.controller;

import com.pfe.predictive.maintenancecost.dto.MaintenanceRecommendationRequest;
import com.pfe.predictive.maintenancecost.dto.MaintenanceRecommendationResponse;
import com.pfe.predictive.maintenancecost.service.MaintenanceRecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Maintenance Recommendation Controller
 * Provides endpoints for generating smart maintenance recommendations
 * 
 * @author Finance Module
 * @version 1.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/maintenance-cost/recommendations")
@RequiredArgsConstructor
@Tag(name = "Maintenance Recommendations", description = "Generate smart maintenance recommendations")
public class MaintenanceRecommendationController {

    private final MaintenanceRecommendationService recommendationService;

    /**
     * Generate maintenance recommendation
     * 
     * POST /api/v1/finance/recommendations/generate
     * 
     * @param request Recommendation request with failure probability and timeline
     * @return Comprehensive maintenance recommendation
     */
    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('FINANCE_MANAGER', 'MAINTENANCE_MANAGER', 'ADMIN')")
    @Operation(summary = "Generate maintenance recommendation", 
               description = "Generate smart maintenance recommendation based on failure probability, cost analysis, and parts availability")
    public ResponseEntity<MaintenanceRecommendationResponse> generateRecommendation(
            @Valid @RequestBody MaintenanceRecommendationRequest request) {
        
        log.info("Generating recommendation - Machine: {}, Probability: {}, Days: {}", 
                 request.getMachineId(), 
                 request.getFailureProbability(), 
                 request.getDaysUntilPredictedFailure());

        MaintenanceRecommendationResponse response = recommendationService.generateRecommendation(
                request.getMachineId(),
                request.getFailureProbability(),
                request.getDaysUntilPredictedFailure(),
                request.getRequiredPartIds()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Get recommendation for a specific machine
     * 
     * GET /api/v1/finance/recommendations/machine/{machineId}
     * 
     * @param machineId Machine ID
     * @param failureProbability Failure probability (0.0 - 1.0)
     * @param daysUntilFailure Days until predicted failure
     * @return Maintenance recommendation
     */
    @GetMapping("/machine/{machineId}")
    @PreAuthorize("hasAnyRole('FINANCE_MANAGER', 'MAINTENANCE_MANAGER', 'ADMIN')")
    @Operation(summary = "Get recommendation for machine", 
               description = "Get maintenance recommendation for a specific machine")
    public ResponseEntity<MaintenanceRecommendationResponse> getRecommendationForMachine(
            @PathVariable Long machineId,
            @RequestParam Double failureProbability,
            @RequestParam Integer daysUntilFailure) {
        
        log.info("Getting recommendation for machine: {}", machineId);

        MaintenanceRecommendationResponse response = recommendationService.generateRecommendation(
                machineId,
                failureProbability,
                daysUntilFailure,
                null
        );

        return ResponseEntity.ok(response);
    }
}
