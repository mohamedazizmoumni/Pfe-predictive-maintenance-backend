package com.pfe.predictive.portal.controller;

import com.pfe.predictive.core.entity.User;
import com.pfe.predictive.portal.dto.PortalMessageRequest;
import com.pfe.predictive.portal.dto.PortalMessageResponse;
import com.pfe.predictive.portal.dto.SupportTicketRequest;
import com.pfe.predictive.portal.dto.SupportTicketResponse;
import com.pfe.predictive.portal.dto.SupportTicketUpdateRequest;
import com.pfe.predictive.portal.service.PortalAccessService;
import com.pfe.predictive.portal.service.PortalMessageService;
import com.pfe.predictive.portal.service.SupportTicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Support tickets — created by customers (row-level scoped to their own
 * linked machines), triaged by Manager/Admin. Same generic-inbox spirit as
 * the rapport/expense/reorder approval flows, kept as its own small
 * controller rather than folded into an existing one since customer and
 * internal roles need very different method-level access here.
 */
@RestController
@RequestMapping("/api/v1/portal/tickets")
@RequiredArgsConstructor
@Tag(name = "Support Tickets", description = "Customer-raised support requests")
public class SupportTicketController {

    private final SupportTicketService supportTicketService;
    private final PortalAccessService portalAccessService;
    private final PortalMessageService portalMessageService;

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Create a support ticket for one of the customer's linked machines")
    public ResponseEntity<SupportTicketResponse> createTicket(
            @Valid @RequestBody SupportTicketRequest request,
            Authentication authentication) {
        User customer = portalAccessService.currentCustomer(authentication);
        SupportTicketResponse response = supportTicketService.createTicket(customer.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "List the authenticated customer's own support tickets")
    public ResponseEntity<Page<SupportTicketResponse>> getMyTickets(
            @PageableDefault(size = 20) Pageable pageable,
            Authentication authentication) {
        User customer = portalAccessService.currentCustomer(authentication);
        return ResponseEntity.ok(supportTicketService.getMyTickets(customer.getId(), pageable));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "List all support tickets (internal triage view)")
    public ResponseEntity<Page<SupportTicketResponse>> getAllTickets(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(supportTicketService.getAllTickets(pageable));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Update a support ticket's status/resolution (internal)")
    public ResponseEntity<SupportTicketResponse> updateTicket(
            @PathVariable Long id,
            @Valid @RequestBody SupportTicketUpdateRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(supportTicketService.updateTicket(id, request, authentication.getName()));
    }

    @PostMapping("/{id}/messages")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Post a message on a ticket's thread (customer must own the ticket; staff can reply to any)")
    public ResponseEntity<PortalMessageResponse> postMessage(
            @PathVariable Long id,
            @Valid @RequestBody PortalMessageRequest request,
            Authentication authentication) {
        boolean isCustomer = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_CUSTOMER"));
        Long callerCustomerUserId = isCustomer ? portalAccessService.currentCustomer(authentication).getId() : null;
        PortalMessageResponse response = portalMessageService.postMessage(
                id, request, authentication.getName(), isCustomer, callerCustomerUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}/messages")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Get a ticket's message thread (customer must own the ticket; staff can view any)")
    public ResponseEntity<List<PortalMessageResponse>> getThread(@PathVariable Long id, Authentication authentication) {
        boolean isCustomer = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_CUSTOMER"));
        Long callerCustomerUserId = isCustomer ? portalAccessService.currentCustomer(authentication).getId() : null;
        return ResponseEntity.ok(portalMessageService.getThread(id, isCustomer, callerCustomerUserId));
    }
}
