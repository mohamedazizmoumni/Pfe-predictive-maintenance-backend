package com.pfe.predictive.passport.controller;

import com.pfe.predictive.passport.dto.MachinePassportResponse;
import com.pfe.predictive.passport.service.MachinePassportService;
import com.pfe.predictive.security.PermissionConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Digital Machine Passport (Priority 3) — one endpoint aggregating a
 * machine's identity, current condition, predictions, recommendations,
 * maintenance history, documents, warranty, and activity timeline. Same
 * read permission as viewing the machine itself (PERM_MACHINE_READ) since
 * this is a composed view of data every one of those roles can already see
 * individually — no new access is granted here.
 */
@RestController
@RequestMapping("/api/v1/machines/{id}/passport")
@RequiredArgsConstructor
@Tag(name = "Machine Passport", description = "Aggregated single-page view of a machine's full digital history")
public class MachinePassportController {

    private final MachinePassportService passportService;

    @GetMapping
    @PreAuthorize(PermissionConstants.PERM_MACHINE_READ)
    @Operation(summary = "Get the Digital Machine Passport for a machine")
    public ResponseEntity<MachinePassportResponse> getPassport(@PathVariable Long id) {
        return ResponseEntity.ok(passportService.forMachine(id));
    }
}
