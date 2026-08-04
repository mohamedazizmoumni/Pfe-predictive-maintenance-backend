package com.pfe.predictive.portal.controller;

import com.pfe.predictive.audit.service.AuditEventService;
import com.pfe.predictive.portal.dto.WarrantyRequest;
import com.pfe.predictive.portal.dto.WarrantyResponse;
import com.pfe.predictive.portal.service.PortalBillingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/portal-admin/warranties")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
@Tag(name = "Customer Portal Admin", description = "Manage machine warranties")
public class PortalAdminWarrantyController {

    private final PortalBillingService portalBillingService;
    private final AuditEventService auditEventService;

    @PostMapping
    @Operation(summary = "Create a warranty record for a machine")
    public ResponseEntity<WarrantyResponse> create(@Valid @RequestBody WarrantyRequest request, Authentication authentication) {
        WarrantyResponse response = portalBillingService.createWarranty(request);
        auditEventService.record(authentication.getName(), "WARRANTY_CREATED", "Warranty", response.getId(),
                "machineId=" + response.getMachineId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/by-machine/{machineId}")
    @Operation(summary = "List warranties for a machine")
    public ResponseEntity<List<WarrantyResponse>> getByMachine(@PathVariable Long machineId) {
        return ResponseEntity.ok(portalBillingService.getWarrantiesForMachine(machineId));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a warranty record")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication authentication) {
        portalBillingService.deleteWarranty(id);
        auditEventService.record(authentication.getName(), "WARRANTY_DELETED", "Warranty", id, null);
        return ResponseEntity.noContent().build();
    }
}
