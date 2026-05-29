package com.pfe.predictive.inventory;

import com.pfe.predictive.inventory.dto.InventoryStatsResponse;
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
}
