package com.pfe.predictive.dashboard.controller;

import com.pfe.predictive.core.entity.Maintenance;
import com.pfe.predictive.core.entity.MaintenanceStatus;
import com.pfe.predictive.data.repository.MaintenanceRepository;
import com.pfe.predictive.dashboard.dto.DashboardSummaryDTO;
import com.pfe.predictive.security.PermissionConstants;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final MaintenanceRepository maintenanceRepository;

    public DashboardController(MaintenanceRepository maintenanceRepository) {
        this.maintenanceRepository = maintenanceRepository;
    }

    @GetMapping("/maintenance")
    @PreAuthorize(PermissionConstants.PERM_DASHBOARD_READ)
    public ResponseEntity<DashboardSummaryDTO.MaintenanceStats> getMaintenanceStats() {
        List<Maintenance> allMaintenance = maintenanceRepository.findAll();

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime weekEnd = now.plusDays(7);
        LocalDateTime monthStart = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);

        long scheduledThisWeek = allMaintenance.stream()
                .filter(m -> m.getScheduledDate() != null
                        && !m.getScheduledDate().isBefore(now)
                        && !m.getScheduledDate().isAfter(weekEnd))
                .count();

        long overdueCount = allMaintenance.stream()
                .filter(m -> m.getScheduledDate() != null
                        && m.getScheduledDate().isBefore(now)
                        && m.getStatus() != MaintenanceStatus.COMPLETED)
                .count();

        List<Maintenance> completedThisMonth = allMaintenance.stream()
                .filter(m -> m.getStatus() == MaintenanceStatus.COMPLETED
                        && m.getCompletedDate() != null
                        && !m.getCompletedDate().isBefore(monthStart))
                .toList();

        long completedCount = completedThisMonth.size();
        double averageCompletionHours = completedThisMonth.stream()
                .filter(m -> m.getStartDate() != null && m.getCompletedDate() != null)
                .mapToLong(m -> java.time.Duration.between(m.getStartDate(), m.getCompletedDate()).toHours())
                .average()
                .orElse(0.0);

        DashboardSummaryDTO.MaintenanceStats response = DashboardSummaryDTO.MaintenanceStats.builder()
                .scheduledThisWeek(scheduledThisWeek)
                .overdueCount(overdueCount)
                .completedThisMonth(completedCount)
                .averageCompletionHours(averageCompletionHours)
                .build();

        return ResponseEntity.ok(response);
    }
}
