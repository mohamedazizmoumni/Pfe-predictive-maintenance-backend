package com.pfe.predictive.fleet.service;

import com.pfe.predictive.common.service.EmailService;
import com.pfe.predictive.common.service.FleetHealthDigestEmailContent;
import com.pfe.predictive.core.entity.MaintenanceType;
import com.pfe.predictive.data.repository.MaintenanceRepository;
import com.pfe.predictive.executive.dto.ExecutiveSummaryDto;
import com.pfe.predictive.executive.service.ExecutiveSummaryService;
import com.pfe.predictive.alert.service.AlertQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Priority 7: aggregates the weekly fleet health digest from existing,
 * already-computed KPI sources (ExecutiveSummaryService, AlertQueryService,
 * MaintenanceRepository) — no new metrics are invented here. Deliberately
 * excludes uptime/downtime: no part of the fleet data model tracks it for
 * real (see Priority 10.3 — "only if data supports it"; it doesn't), so
 * including it would mean fabricating a number.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FleetHealthDigestService {

    private static final DateTimeFormatter PERIOD_DATE_FORMAT = DateTimeFormatter.ofPattern("MMM d, yyyy");

    private final ExecutiveSummaryService executiveSummaryService;
    private final AlertQueryService alertQueryService;
    private final MaintenanceRepository maintenanceRepository;
    private final EmailService emailService;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    public void sendDigest() {
        LocalDateTime periodEnd = LocalDateTime.now();
        LocalDateTime periodStart = periodEnd.minusDays(7);

        ExecutiveSummaryDto summary = executiveSummaryService.getSummary();

        long newAlertsThisWeek = alertQueryService
                .getAlertsByDateRange(periodStart, periodEnd, Pageable.unpaged())
                .getTotalElements();
        long resolvedAlertsThisWeek = alertQueryService
                .getRecentlyClosedAlerts(7 * 24, Pageable.unpaged())
                .getTotalElements();
        long unresolvedAlerts = alertQueryService.countUnresolvedAlerts();
        Double averageResolutionTimeHours = alertQueryService.getAverageResolutionTimeHours();

        long preventiveCompleted = maintenanceRepository.countByTypeAndCompletedDateAfter(MaintenanceType.PREVENTIVE, periodStart);
        long correctiveCompleted = maintenanceRepository.countByTypeAndCompletedDateAfter(MaintenanceType.CORRECTIVE, periodStart)
                + maintenanceRepository.countByTypeAndCompletedDateAfter(MaintenanceType.EMERGENCY, periodStart);

        List<FleetHealthDigestEmailContent.TopRisk> topRisks = summary.getTopReliabilityRisks() == null
                ? List.of()
                : summary.getTopReliabilityRisks().stream()
                        .map(risk -> new FleetHealthDigestEmailContent.TopRisk(
                                risk.getMachineName(), risk.getMtbfHours(), risk.getFailureCount()))
                        .toList();

        FleetHealthDigestEmailContent content = new FleetHealthDigestEmailContent(
                periodStart.format(PERIOD_DATE_FORMAT) + " - " + periodEnd.format(PERIOD_DATE_FORMAT),
                summary.getMachineCount(),
                summary.getFleetAverageHealth(),
                summary.getOpenWorkOrders(),
                summary.getOverdueWorkOrders(),
                summary.getBudgetUtilizationPercentage(),
                unresolvedAlerts,
                newAlertsThisWeek,
                resolvedAlertsThisWeek,
                averageResolutionTimeHours,
                preventiveCompleted,
                correctiveCompleted,
                topRisks,
                frontendUrl != null && !frontendUrl.isBlank() ? frontendUrl.trim() : "http://localhost:4200"
        );

        log.info("Sending weekly fleet health digest — machines={}, openWorkOrders={}, unresolvedAlerts={}",
                content.machineCount(), content.openWorkOrders(), content.unresolvedAlerts());
        emailService.sendFleetHealthDigest(content);
    }
}
