package com.pfe.predictive.finance.controller;

import com.pfe.predictive.finance.dto.MaintenanceRapportRequest;
import com.pfe.predictive.finance.dto.MaintenanceRapportResponse;
import com.pfe.predictive.finance.dto.RapportApprovalRequest;
import com.pfe.predictive.finance.service.MaintenanceRapportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/finance/rapports")
@RequiredArgsConstructor
@Tag(name = "Finance — Maintenance Rapports", description = "Technician job rapports requiring manager then finance approval")
public class MaintenanceRapportController {

    private final MaintenanceRapportService rapportService;

    // ─────────────────────────────────────────────────────────────────────────
    // CREATE
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Submit a new maintenance rapport")
    public ResponseEntity<MaintenanceRapportResponse> createRapport(
            @Valid @RequestBody MaintenanceRapportRequest request,
            Authentication authentication) {

        String username = authentication.getName();
        log.info("POST /finance/rapports — user={}", username);

        MaintenanceRapportResponse response = rapportService.createRapport(request, username, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // READ
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/mine")
    @Operation(summary = "Get my submitted rapports")
    public ResponseEntity<List<MaintenanceRapportResponse>> getMyRapports(Authentication authentication) {
        return ResponseEntity.ok(rapportService.getMyRapports(authentication.getName()));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('FINANCE_MANAGER', 'MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Get all maintenance rapports")
    public ResponseEntity<List<MaintenanceRapportResponse>> getAllRapports() {
        return ResponseEntity.ok(rapportService.getAllRapports());
    }

    @GetMapping("/pending-manager")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Get rapports awaiting manager approval")
    public ResponseEntity<List<MaintenanceRapportResponse>> getPendingManagerApprovals() {
        return ResponseEntity.ok(rapportService.getPendingManagerApprovals());
    }

    @GetMapping("/pending-finance")
    @PreAuthorize("hasAnyRole('FINANCE_MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Get rapports awaiting finance approval")
    public ResponseEntity<List<MaintenanceRapportResponse>> getPendingFinanceApprovals() {
        return ResponseEntity.ok(rapportService.getPendingFinanceApprovals());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('FINANCE_MANAGER', 'MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Get a rapport by ID (submitters use /mine for their own rapports)")
    public ResponseEntity<MaintenanceRapportResponse> getRapportById(@PathVariable Long id) {
        return ResponseEntity.ok(rapportService.getRapportById(id));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // APPROVAL WORKFLOW
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/{id}/manager-approval")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Manager approves or rejects a pending rapport")
    public ResponseEntity<MaintenanceRapportResponse> managerApproval(
            @PathVariable Long id,
            @RequestBody RapportApprovalRequest request,
            Authentication authentication) {

        log.info("POST /finance/rapports/{}/manager-approval — reviewer={}", id, authentication.getName());
        return ResponseEntity.ok(rapportService.reviewByManager(id, request, authentication.getName()));
    }

    @PostMapping("/{id}/finance-approval")
    @PreAuthorize("hasAnyRole('FINANCE_MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Finance manager approves or rejects a manager-approved rapport")
    public ResponseEntity<MaintenanceRapportResponse> financeApproval(
            @PathVariable Long id,
            @RequestBody RapportApprovalRequest request,
            Authentication authentication) {

        log.info("POST /finance/rapports/{}/finance-approval — reviewer={}", id, authentication.getName());
        return ResponseEntity.ok(rapportService.reviewByFinance(id, request, authentication.getName()));
    }
}
