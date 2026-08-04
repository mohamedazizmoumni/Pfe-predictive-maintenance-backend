package com.pfe.predictive.inventory;

import com.pfe.predictive.audit.service.AuditEventService;
import com.pfe.predictive.inventory.dto.SupplierRequest;
import com.pfe.predictive.inventory.dto.SupplierResponse;
import com.pfe.predictive.inventory.dto.SupplierScorecardResponse;
import com.pfe.predictive.inventory.service.SupplierService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Supplier directory + scorecard — Stock Manager extended with Procurement
 * Officer responsibilities (2026-08-01 scope reorientation).
 */
@RestController
@RequestMapping("/api/v1/inventory/suppliers")
@RequiredArgsConstructor
@Tag(name = "Suppliers", description = "Supplier directory and delivery scorecards")
public class SupplierController {

    private final SupplierService supplierService;
    private final AuditEventService auditEventService;

    @PostMapping
    @PreAuthorize("hasAnyRole('STOCK_MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Create a supplier")
    public ResponseEntity<SupplierResponse> create(@Valid @RequestBody SupplierRequest request, Authentication authentication) {
        SupplierResponse response = supplierService.create(request);
        auditEventService.record(authentication.getName(), "SUPPLIER_CREATED", "Supplier", response.getId(), "name=" + response.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('STOCK_MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Update a supplier")
    public ResponseEntity<SupplierResponse> update(@PathVariable Long id, @Valid @RequestBody SupplierRequest request, Authentication authentication) {
        SupplierResponse response = supplierService.update(id, request);
        auditEventService.record(authentication.getName(), "SUPPLIER_UPDATED", "Supplier", id, null);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('STOCK_MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Deactivate a supplier (soft delete — historical orders keep referencing it)")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication authentication) {
        supplierService.delete(id);
        auditEventService.record(authentication.getName(), "SUPPLIER_DEACTIVATED", "Supplier", id, null);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('STOCK_MANAGER', 'MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "List suppliers")
    public ResponseEntity<List<SupplierResponse>> getAll(@RequestParam(defaultValue = "false") boolean activeOnly) {
        return ResponseEntity.ok(supplierService.getAll(activeOnly));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STOCK_MANAGER', 'MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Get a supplier by ID")
    public ResponseEntity<SupplierResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(supplierService.getById(id));
    }

    @GetMapping("/{id}/scorecard")
    @PreAuthorize("hasAnyRole('STOCK_MANAGER', 'MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Get a supplier's delivery performance scorecard")
    public ResponseEntity<SupplierScorecardResponse> getScorecard(@PathVariable Long id) {
        return ResponseEntity.ok(supplierService.getScorecard(id));
    }
}
