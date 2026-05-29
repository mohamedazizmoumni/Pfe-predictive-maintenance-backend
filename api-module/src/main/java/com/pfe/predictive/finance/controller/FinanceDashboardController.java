package com.pfe.predictive.finance.controller;

import com.pfe.predictive.finance.dto.FinanceDashboardResponse;
import com.pfe.predictive.finance.service.FinanceDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/finance/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('FINANCE_MANAGER', 'ADMIN', 'SUPER_ADMIN')")
@Tag(name = "Finance — Dashboard", description = "Finance overview: budget utilization and expense statistics")
public class FinanceDashboardController {

    private final FinanceDashboardService dashboardService;

    @GetMapping
    @Operation(summary = "Get the finance dashboard for the current year")
    public ResponseEntity<FinanceDashboardResponse> getDashboard() {
        log.info("GET /finance/dashboard");
        return ResponseEntity.ok(dashboardService.getDashboard());
    }
}
