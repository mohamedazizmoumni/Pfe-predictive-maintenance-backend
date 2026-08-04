package com.pfe.predictive.portal.controller;

import com.pfe.predictive.core.entity.User;
import com.pfe.predictive.portal.dto.InvoiceResponse;
import com.pfe.predictive.portal.dto.WarrantyResponse;
import com.pfe.predictive.portal.service.PortalAccessService;
import com.pfe.predictive.portal.service.PortalBillingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Customer-facing warranty/invoice reads, scoped to the authenticated
 * customer's own linked machines / own invoices.
 */
@RestController
@RequestMapping("/api/v1/portal")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
@Tag(name = "Customer Portal Billing", description = "Warranties and invoices visible to the authenticated customer")
public class PortalBillingController {

    private final PortalBillingService portalBillingService;
    private final PortalAccessService portalAccessService;

    @GetMapping("/warranties")
    @Operation(summary = "List warranties for the customer's linked machines")
    public ResponseEntity<List<WarrantyResponse>> getMyWarranties(Authentication authentication) {
        User customer = portalAccessService.currentCustomer(authentication);
        return ResponseEntity.ok(portalBillingService.getWarrantiesForCustomer(customer.getId()));
    }

    @GetMapping("/invoices")
    @Operation(summary = "List the customer's own invoices")
    public ResponseEntity<List<InvoiceResponse>> getMyInvoices(Authentication authentication) {
        User customer = portalAccessService.currentCustomer(authentication);
        return ResponseEntity.ok(portalBillingService.getInvoicesForCustomer(customer.getId()));
    }
}
