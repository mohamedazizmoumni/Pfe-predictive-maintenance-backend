package com.pfe.predictive.reliability.controller;

import com.pfe.predictive.reliability.dto.FailureRecordDto;
import com.pfe.predictive.reliability.dto.MachineReliabilityDto;
import com.pfe.predictive.reliability.service.ReliabilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * MTBF/MTTR/failure-analytics for Manager. This is the "Reliability Engineer"
 * responsibility set from the enterprise blueprint, folded into Manager per
 * the 2026-08-01 scope reorientation rather than a new role.
 */
@RestController
@RequestMapping("/api/v1/reliability")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN', 'SUPER_ADMIN')")
@Tag(name = "Reliability", description = "MTBF/MTTR and failure/root-cause analytics")
public class ReliabilityController {

    private final ReliabilityService reliabilityService;

    @GetMapping("/fleet")
    @Operation(summary = "Fleet-wide MTBF/MTTR ranking, worst reliability first")
    public ResponseEntity<List<MachineReliabilityDto>> getFleetReliability() {
        return ResponseEntity.ok(reliabilityService.getFleetReliability());
    }

    @GetMapping("/failures")
    @Operation(summary = "Recent corrective/emergency repairs with root cause, optionally filtered by machine")
    public ResponseEntity<List<FailureRecordDto>> getFailureRecords(
            @RequestParam(required = false) Long machineId) {
        return ResponseEntity.ok(reliabilityService.getFailureRecords(machineId));
    }
}
