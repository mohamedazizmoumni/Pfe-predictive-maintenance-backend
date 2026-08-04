package com.pfe.predictive.portal.controller;

import com.pfe.predictive.audit.service.AuditEventService;
import com.pfe.predictive.core.entity.portal.InvoiceStatus;
import com.pfe.predictive.portal.dto.InvoiceRequest;
import com.pfe.predictive.portal.dto.InvoiceResponse;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/portal-admin/invoices")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'FINANCE_MANAGER')")
@Tag(name = "Customer Portal Admin", description = "Manage customer invoices")
public class PortalAdminInvoiceController {

    private final PortalBillingService portalBillingService;
    private final AuditEventService auditEventService;

    @PostMapping
    @Operation(summary = "Create an invoice for a customer")
    public ResponseEntity<InvoiceResponse> create(@Valid @RequestBody InvoiceRequest request, Authentication authentication) {
        InvoiceResponse response = portalBillingService.createInvoice(request);
        auditEventService.record(authentication.getName(), "INVOICE_CREATED", "Invoice", response.getId(),
                "customerUserId=" + response.getCustomerUserId() + "; amount=" + response.getAmount());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "List all invoices")
    public ResponseEntity<List<InvoiceResponse>> getAll() {
        return ResponseEntity.ok(portalBillingService.getAllInvoices());
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Update an invoice's status")
    public ResponseEntity<InvoiceResponse> updateStatus(@PathVariable Long id, @RequestBody InvoiceStatus status, Authentication authentication) {
        InvoiceResponse response = portalBillingService.updateInvoiceStatus(id, status);
        auditEventService.record(authentication.getName(), "INVOICE_STATUS_UPDATED", "Invoice", id, "status=" + status);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an invoice")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication authentication) {
        portalBillingService.deleteInvoice(id);
        auditEventService.record(authentication.getName(), "INVOICE_DELETED", "Invoice", id, null);
        return ResponseEntity.noContent().build();
    }
}
