package com.pfe.predictive.portal.controller;

import com.pfe.predictive.core.entity.User;
import com.pfe.predictive.portal.dto.PortalMachineDetailDto;
import com.pfe.predictive.portal.dto.PortalMachineSummaryDto;
import com.pfe.predictive.portal.dto.PortalMaintenanceHistoryDto;
import com.pfe.predictive.portal.service.PortalAccessService;
import com.pfe.predictive.portal.service.PortalMachineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Customer Portal — read-only fleet view, hard-scoped to the authenticated
 * customer's linked machines (see PortalAccessService / CustomerMachineLink).
 * Every value returned is plain-language; no raw risk scores or model
 * internals cross this boundary.
 */
@RestController
@RequestMapping("/api/v1/portal/machines")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
@Tag(name = "Customer Portal", description = "Row-level-scoped read access for external equipment owners")
public class PortalMachineController {

    private final PortalMachineService portalMachineService;
    private final PortalAccessService portalAccessService;

    @GetMapping
    @Operation(summary = "List machines linked to the authenticated customer")
    public ResponseEntity<List<PortalMachineSummaryDto>> listMyMachines(Authentication authentication) {
        User customer = portalAccessService.currentCustomer(authentication);
        return ResponseEntity.ok(portalMachineService.listMyMachines(customer.getId()));
    }

    @GetMapping("/{machineId}")
    @Operation(summary = "Get plain-language detail for one of the customer's linked machines")
    public ResponseEntity<PortalMachineDetailDto> getMachineDetail(
            @PathVariable Long machineId,
            Authentication authentication) {
        User customer = portalAccessService.currentCustomer(authentication);
        return ResponseEntity.ok(portalMachineService.getMachineDetail(customer.getId(), machineId));
    }

    @GetMapping("/{machineId}/maintenance-history")
    @Operation(summary = "Get maintenance history for one of the customer's linked machines")
    public ResponseEntity<Page<PortalMaintenanceHistoryDto>> getMaintenanceHistory(
            @PathVariable Long machineId,
            @PageableDefault(size = 20) Pageable pageable,
            Authentication authentication) {
        User customer = portalAccessService.currentCustomer(authentication);
        return ResponseEntity.ok(portalMachineService.getMaintenanceHistory(customer.getId(), machineId, pageable));
    }
}
