package com.pfe.predictive.portal.controller;

import com.pfe.predictive.audit.service.AuditEventService;
import com.pfe.predictive.core.entity.Role;
import com.pfe.predictive.core.entity.User;
import com.pfe.predictive.core.entity.portal.CustomerMachineLink;
import com.pfe.predictive.data.repository.UserRepository;
import com.pfe.predictive.data.repository.portal.CustomerMachineLinkRepository;
import com.pfe.predictive.portal.dto.CustomerMachineLinkRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
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

/**
 * Admin-side management of which machines a customer account can see.
 * Deliberately separate from the customer-facing /portal/** controllers —
 * this is an internal admin tool, not part of the portal itself.
 */
@RestController
@RequestMapping("/api/v1/portal-admin/links")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
@Tag(name = "Customer Portal Admin", description = "Manage which machines a customer account can see")
public class PortalAdminController {

    private final CustomerMachineLinkRepository linkRepository;
    private final UserRepository userRepository;
    private final AuditEventService auditEventService;

    @PostMapping
    @Operation(summary = "Link a customer account to a machine")
    public ResponseEntity<CustomerMachineLink> createLink(
            @Valid @RequestBody CustomerMachineLinkRequest request,
            Authentication authentication) {

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + request.getUserId()));
        boolean isCustomer = user.getRoles() != null && user.getRoles().stream()
                .map(Role::getName)
                .anyMatch(name -> name.equalsIgnoreCase("CUSTOMER") || name.equalsIgnoreCase("ROLE_CUSTOMER"));
        if (!isCustomer) {
            return ResponseEntity.badRequest().build();
        }

        if (linkRepository.existsByUserIdAndMachineId(request.getUserId(), request.getMachineId())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        CustomerMachineLink link = CustomerMachineLink.builder()
                .userId(request.getUserId())
                .machineId(request.getMachineId())
                .linkedBy(authentication.getName())
                .build();
        CustomerMachineLink saved = linkRepository.save(link);
        auditEventService.record(authentication.getName(), "CUSTOMER_MACHINE_LINKED", "CustomerMachineLink", saved.getId(),
                "userId=" + request.getUserId() + "; machineId=" + request.getMachineId());
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping("/by-user/{userId}")
    @Operation(summary = "List machines linked to a customer account")
    public ResponseEntity<List<CustomerMachineLink>> listByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(linkRepository.findByUserId(userId));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove a customer-machine link")
    public ResponseEntity<Void> deleteLink(@PathVariable Long id, Authentication authentication) {
        if (!linkRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        linkRepository.deleteById(id);
        auditEventService.record(authentication.getName(), "CUSTOMER_MACHINE_UNLINKED", "CustomerMachineLink", id, null);
        return ResponseEntity.noContent().build();
    }
}
