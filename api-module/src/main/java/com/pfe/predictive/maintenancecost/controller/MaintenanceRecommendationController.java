package com.pfe.predictive.maintenancecost.controller;

import com.pfe.predictive.core.entity.MaintenanceRecommendationStatus;
import com.pfe.predictive.maintenancecost.dto.MaintenanceRecommendationRequest;
import com.pfe.predictive.maintenancecost.dto.MaintenanceRecommendationResponse;
import com.pfe.predictive.maintenancecost.dto.RecommendationDecisionRequest;
import com.pfe.predictive.maintenancecost.dto.SavedRecommendationResponse;
import com.pfe.predictive.maintenancecost.service.MaintenanceRecommendationApprovalService;
import com.pfe.predictive.maintenancecost.service.MaintenanceRecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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
    private final MaintenanceRecommendationApprovalService approvalService;

    /**
     * Preview-only cost/parts estimate. Resolves {@code machineId} against
     * {@link com.pfe.predictive.maintenancecost.entity.Machine} — a separate,
     * sparsely-seeded table ({@code maintenance_cost_machines}, ~3 demo rows),
     * NOT the real {@code machines} table the rest of the app uses. Any real
     * machine outside those demo rows 404s here.
     * <p>
     * <b>Superseded for the real generate/approve/reject workflow</b> by
     * {@code POST /generate-and-save} + {@code GET /history} +
     * {@code PUT /{id}/approve}/{@code /reject} below, which resolve against
     * the real {@link com.pfe.predictive.core.entity.Machine} instead. No
     * frontend code calls this method as of the Priority-0.1 fix — kept only
     * because {@link CostComparisonController} still shares the legacy
     * cost-model tables and this method's cost-estimate logic. Do not wire
     * new frontend code to this endpoint; use the saved-recommendation flow.
     *
     * POST /api/v1/maintenance-cost/recommendations/generate
     *
     * @param request Recommendation request with failure probability and timeline
     * @return Comprehensive maintenance recommendation (preview only, not persisted)
     */
    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('FINANCE_MANAGER', 'MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "[Legacy preview, real machines outside 3 demo rows will 404] Generate maintenance recommendation",
               description = "Superseded by /generate-and-save for real machines - see class Javadoc on this method")
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
     * GET /api/v1/maintenance-cost/recommendations/machine/{machineId}
     *
     * @param machineId Machine ID
     * @param failureProbability Failure probability (0.0 - 1.0); defaults to a
     *        moderate-risk estimate when the caller has no real prediction yet
     * @param daysUntilFailure Days until predicted failure; same fallback
     * @return Maintenance recommendation
     */
    // Same legacy-table caveat as /generate above — superseded by /generate-and-save.
    @GetMapping("/machine/{machineId}")
    @PreAuthorize("hasAnyRole('FINANCE_MANAGER', 'MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "[Legacy preview, real machines outside 3 demo rows will 404] Get recommendation for machine",
               description = "Superseded by /generate-and-save for real machines - see class Javadoc on /generate")
    public ResponseEntity<MaintenanceRecommendationResponse> getRecommendationForMachine(
            @PathVariable Long machineId,
            @RequestParam(required = false, defaultValue = "0.5") Double failureProbability,
            @RequestParam(required = false, defaultValue = "30") Integer daysUntilFailure) {
        
        log.info("Getting recommendation for machine: {}", machineId);

        MaintenanceRecommendationResponse response = recommendationService.generateRecommendation(
                machineId,
                failureProbability,
                daysUntilFailure,
                null
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Generate a recommendation AND persist it for approval/history — unlike
     * /generate and /machine/{id}, which stay pure previews, this is the
     * entry point into the approve/reject workflow.
     */
    @PostMapping("/generate-and-save")
    @PreAuthorize("hasAnyRole('FINANCE_MANAGER', 'MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Generate and save a recommendation for approval")
    public ResponseEntity<SavedRecommendationResponse> generateAndSave(
            @Valid @RequestBody MaintenanceRecommendationRequest request,
            Authentication authentication) {
        SavedRecommendationResponse response = approvalService.generateAndSave(
                request.getMachineId(),
                request.getFailureProbability(),
                request.getDaysUntilPredictedFailure(),
                authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('FINANCE_MANAGER', 'MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "List saved recommendations, optionally filtered by status")
    public ResponseEntity<Page<SavedRecommendationResponse>> history(
            @RequestParam(required = false) MaintenanceRecommendationStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        return ResponseEntity.ok(approvalService.history(status, pageable));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('FINANCE_MANAGER', 'MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Approve a saved recommendation, creating a Maintenance work order (unless MONITOR)")
    public ResponseEntity<SavedRecommendationResponse> approve(
            @PathVariable Long id,
            @RequestBody(required = false) RecommendationDecisionRequest request,
            Authentication authentication) {
        String note = request != null ? request.getNote() : null;
        return ResponseEntity.ok(approvalService.approve(id, authentication.getName(), note));
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('FINANCE_MANAGER', 'MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Reject a saved recommendation (a reason is required)")
    public ResponseEntity<SavedRecommendationResponse> reject(
            @PathVariable Long id,
            @RequestBody RecommendationDecisionRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(approvalService.reject(id, authentication.getName(), request.getNote()));
    }
}
