package com.pfe.predictive.inventory;

import com.pfe.predictive.inventory.dto.InventoryStatsResponse;
import com.pfe.predictive.inventory.dto.InventoryUsageResponse;
import com.pfe.predictive.inventory.dto.LowStockAlertResponse;
import com.pfe.predictive.inventory.dto.ReorderSummaryResponse;
import com.pfe.predictive.inventory.service.InventoryAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Inventory Analytics Controller
 * Provides inventory statistics and insights
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/inventory/analytics")
@RequiredArgsConstructor
@Tag(name = "Inventory Analytics", description = "Inventory statistics and analytics")
public class InventoryAnalyticsController {
    
    private final InventoryAnalyticsService analyticsService;
    
    /**
     * Get overall inventory statistics
     */
    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('STOCK_MANAGER', 'MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Get inventory stats", description = "Retrieve overall inventory statistics")
    public ResponseEntity<InventoryStatsResponse> getInventoryStats() {
        log.info("Fetching inventory statistics");
        InventoryStatsResponse stats = analyticsService.getInventoryStats();
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Get low stock alerts
     */
    @GetMapping("/low-stock-alerts")
    @PreAuthorize("hasAnyRole('STOCK_MANAGER', 'MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Get low stock alerts", description = "Retrieve parts with low stock levels")
    public ResponseEntity<List<LowStockAlertResponse>> getLowStockAlerts() {
        log.info("Fetching low stock alerts");
        List<LowStockAlertResponse> alerts = analyticsService.getLowStockAlerts();
        return ResponseEntity.ok(alerts);
    }
    
    /**
     * Get critical reorder requests
     */
    @GetMapping("/critical-reorders")
    @PreAuthorize("hasAnyRole('STOCK_MANAGER', 'MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Get critical reorders", description = "Retrieve critical pending reorder requests")
    public ResponseEntity<List<ReorderSummaryResponse>> getCriticalReorders(
            @RequestParam(defaultValue = "10") int limit) {
        
        log.info("Fetching critical reorders (limit: {})", limit);
        List<ReorderSummaryResponse> reorders = analyticsService.getCriticalReorders(limit);
        return ResponseEntity.ok(reorders);
    }

    /**
     * Get real recorded usage history for a part. Returns an empty list when no
     * technician has logged consumption of this part yet — the frontend should
     * treat that as "no real data available", not as zero usage.
     */
    @GetMapping("/usage/{partId}")
    @PreAuthorize("hasAnyRole('STOCK_MANAGER', 'MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Get part usage history", description = "Retrieve real recorded usage for a part over a trailing window")
    public ResponseEntity<List<InventoryUsageResponse>> getUsageForPart(
            @PathVariable Long partId,
            @RequestParam(defaultValue = "90") int days) {

        log.info("Fetching usage history for part {} (last {} days)", partId, days);
        return ResponseEntity.ok(analyticsService.getUsageForPart(partId, days));
    }

    /**
     * Real usage across every part in one call, so the frontend can compute
     * per-part consumption rates without an HTTP round trip per part.
     */
    @GetMapping("/usage")
    @PreAuthorize("hasAnyRole('STOCK_MANAGER', 'MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Get usage history for all parts", description = "Retrieve real recorded usage across all parts over a trailing window")
    public ResponseEntity<List<InventoryUsageResponse>> getAllUsage(@RequestParam(defaultValue = "90") int days) {
        log.info("Fetching usage history for all parts (last {} days)", days);
        return ResponseEntity.ok(analyticsService.getAllUsage(days));
    }
}
