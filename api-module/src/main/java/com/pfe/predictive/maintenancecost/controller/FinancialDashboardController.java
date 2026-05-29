package com.pfe.predictive.maintenancecost.controller;

import com.pfe.predictive.maintenancecost.dto.FailureReportResponse;
import com.pfe.predictive.maintenancecost.dto.FinancialDashboardResponse;
import com.pfe.predictive.maintenancecost.dto.MaintenanceReportResponse;
import com.pfe.predictive.maintenancecost.service.FinancialDashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Financial Dashboard Controller
 * Provides comprehensive financial analytics
 * 
 * Security: Accessible by FINANCE_MANAGER, MANAGER, ADMIN
 * 
 * @author Finance Module
 * @version 1.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/maintenance-cost")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('FINANCE_MANAGER', 'MANAGER', 'ADMIN', 'SUPER_ADMIN')")
public class FinancialDashboardController {

    private final FinancialDashboardService dashboardService;

    /**
     * Get comprehensive financial dashboard
     * GET /api/v1/finance/dashboard
     * 
     * Returns:
     * - Total allocated/spent/remaining
     * - Top cost machines
     * - Budget alerts
     * - Monthly costs
     * - Budgets by department
     */
    @GetMapping("/dashboard")
    public ResponseEntity<FinancialDashboardResponse> getDashboard() {
        log.info("📊 Fetching financial dashboard");
        FinancialDashboardResponse dashboard = dashboardService.getDashboard();
        return ResponseEntity.ok(dashboard);
    }

    /**
     * Get failure report
     * GET /api/v1/finance/reports/failures
     * 
     * Returns:
     * - Total failures
     * - Total cost
     * - Total downtime
     * - Detailed failure list
     */
    @GetMapping("/reports/failures")
    public ResponseEntity<FailureReportResponse> getFailureReport() {
        log.info("📋 Fetching failure report");
        FailureReportResponse report = dashboardService.getFailureReport();
        return ResponseEntity.ok(report);
    }

    /**
     * Get maintenance report
     * GET /api/v1/finance/reports/maintenance
     * 
     * Returns:
     * - Total maintenance actions
     * - Total cost (labor + parts)
     * - Detailed action list
     */
    @GetMapping("/reports/maintenance")
    public ResponseEntity<MaintenanceReportResponse> getMaintenanceReport() {
        log.info("📋 Fetching maintenance report");
        MaintenanceReportResponse report = dashboardService.getMaintenanceReport();
        return ResponseEntity.ok(report);
    }

    /**
     * Get top cost machines
     * GET /api/v1/finance/reports/top-cost-machines
     */
    @GetMapping("/reports/top-cost-machines")
    public ResponseEntity<?> getTopCostMachines() {
        log.info("📊 Fetching top cost machines");
        var topMachines = dashboardService.getTopCostMachines(10);
        return ResponseEntity.ok(topMachines);
    }

    /**
     * Get failure summary report
     * GET /api/v1/finance/reports/failure/summary
     * 
     * Returns:
     * - Total failures count
     * - Total failure cost
     * - Average cost per failure
     * - Most common failure types
     */
    @GetMapping("/reports/failure/summary")
    public ResponseEntity<?> getFailureSummary() {
        log.info("📊 Fetching failure summary");
        var summary = dashboardService.getFailureSummary();
        return ResponseEntity.ok(summary);
    }
}
