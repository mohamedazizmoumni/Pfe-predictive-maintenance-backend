package com.pfe.predictive.template.controller;

import com.pfe.predictive.template.dto.RecurringMaintenanceRuleRequest;
import com.pfe.predictive.template.dto.RecurringMaintenanceRuleResponse;
import com.pfe.predictive.template.service.RecurringMaintenanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/recurring-maintenance")
@RequiredArgsConstructor
public class RecurringMaintenanceController {

    private final RecurringMaintenanceService recurringMaintenanceService;

    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<RecurringMaintenanceRuleResponse> create(@Valid @RequestBody RecurringMaintenanceRuleRequest request,
                                                                     Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(recurringMaintenanceService.create(request, authentication.getName()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        recurringMaintenanceService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/machine/{machineId}")
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<List<RecurringMaintenanceRuleResponse>> getByMachine(@PathVariable Long machineId) {
        return ResponseEntity.ok(recurringMaintenanceService.getByMachine(machineId));
    }
}
