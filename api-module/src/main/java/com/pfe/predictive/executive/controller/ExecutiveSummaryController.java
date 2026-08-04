package com.pfe.predictive.executive.controller;

import com.pfe.predictive.executive.dto.ExecutiveSummaryDto;
import com.pfe.predictive.executive.service.ExecutiveSummaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/executive-summary")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
@Tag(name = "Executive Summary", description = "Cross-module KPI rollup for the executive dashboard widget")
public class ExecutiveSummaryController {

    private final ExecutiveSummaryService executiveSummaryService;

    @GetMapping
    @Operation(summary = "Get the cross-module executive KPI summary")
    public ResponseEntity<ExecutiveSummaryDto> getSummary() {
        return ResponseEntity.ok(executiveSummaryService.getSummary());
    }
}
