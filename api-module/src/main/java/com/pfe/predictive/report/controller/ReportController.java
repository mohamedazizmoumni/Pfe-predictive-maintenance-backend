package com.pfe.predictive.report.controller;

import com.pfe.predictive.report.service.MaintenanceInterventionReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Priority 5: server-side PDF report generation — the first report type
 * (Maintenance Intervention Report). Stays a plain download endpoint;
 * Priority 6 wires this same PDF into an email instead of only a manual
 * download.
 */
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Server-side PDF report generation")
public class ReportController {

    private final MaintenanceInterventionReportService interventionReportService;

    @GetMapping("/maintenance-rapports/{id}")
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'MANAGER', 'FINANCE_MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Download the Maintenance Intervention Report PDF for a rapport")
    public ResponseEntity<byte[]> maintenanceInterventionReport(@PathVariable Long id) {
        byte[] pdf = interventionReportService.generate(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(
                ContentDisposition.attachment().filename("maintenance-report-" + id + ".pdf").build());

        return ResponseEntity.ok().headers(headers).body(pdf);
    }
}
