package com.pfe.predictive.template.controller;

import com.pfe.predictive.template.dto.WorkOrderTemplateRequest;
import com.pfe.predictive.template.dto.WorkOrderTemplateResponse;
import com.pfe.predictive.template.service.WorkOrderTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/work-order-templates")
@RequiredArgsConstructor
public class WorkOrderTemplateController {

    private final WorkOrderTemplateService templateService;

    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<WorkOrderTemplateResponse> create(@Valid @RequestBody WorkOrderTemplateRequest request,
                                                              Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(templateService.create(request, authentication.getName()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<WorkOrderTemplateResponse> update(@PathVariable Long id,
                                                              @Valid @RequestBody WorkOrderTemplateRequest request) {
        return ResponseEntity.ok(templateService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        templateService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<List<WorkOrderTemplateResponse>> getAll(
            @RequestParam(value = "activeOnly", defaultValue = "true") boolean activeOnly) {
        return ResponseEntity.ok(templateService.getAll(activeOnly));
    }
}
