package com.pfe.predictive.inventory.service;

import com.pfe.predictive.inventory.dto.InventoryStatsResponse;
import com.pfe.predictive.inventory.dto.InventoryUsageResponse;
import com.pfe.predictive.inventory.dto.LowStockAlertResponse;
import com.pfe.predictive.inventory.dto.ReorderSummaryResponse;
import com.pfe.predictive.inventory.entity.InventoryUsage;
import com.pfe.predictive.inventory.entity.PartStatus;
import com.pfe.predictive.inventory.entity.ReorderStatus;
import com.pfe.predictive.inventory.entity.StockOrderStatus;
import com.pfe.predictive.inventory.mapper.InventoryUsageMapper;
import com.pfe.predictive.inventory.repository.InventoryUsageRepository;
import com.pfe.predictive.inventory.repository.PartRepository;
import com.pfe.predictive.inventory.repository.ReorderRequestRepository;
import com.pfe.predictive.inventory.repository.StockOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryAnalyticsService {

    private final PartRepository partRepository;
    private final ReorderRequestRepository reorderRepository;
    private final StockOrderRepository stockOrderRepository;
    private final InventoryUsageRepository inventoryUsageRepository;
    private final InventoryUsageMapper inventoryUsageMapper;

    public long countTotalParts() {
        return partRepository.count();
    }

    public long countLowStockParts() {
        return partRepository.countByStatus(PartStatus.LOW_STOCK);
    }

    public long countOutOfStockParts() {
        return partRepository.countByStatus(PartStatus.OUT_OF_STOCK);
    }

    public long countPendingReorderApprovals() {
        return reorderRepository.countByStatus(ReorderStatus.REQUESTED);
    }

    public long countPendingOrders() {
        return stockOrderRepository.countByStatus(StockOrderStatus.PENDING);
    }

    public InventoryStatsResponse getInventoryStats() {
        return InventoryStatsResponse.builder()
            .totalPartsTracked(countTotalParts())
            .lowStockPartsCount(countLowStockParts())
            .outOfStockPartsCount(countOutOfStockParts())
            .pendingOrdersCount(countPendingOrders())
            .turnoverRate(getStockTurnoverRate())
            .lastUpdated(String.valueOf(System.currentTimeMillis()))
            .build();
    }

    public List<LowStockAlertResponse> getLowStockAlerts() {
        return partRepository.findLowStockParts()
            .stream()
            .limit(10)
            .map(part -> LowStockAlertResponse.builder()
                .partId(part.getId())
                .partName(part.getName())
                .currentStock(part.getCurrentStock())
                .minimumStock(part.getMinimumStock())
                .status(part.getStatus().toString())
                .build())
            .toList();
    }

    /**
     * Annualized turnover rate = usage over the last 365 days / current stock on hand.
     * Returns 0 when there is no recorded usage yet or no stock to divide by.
     */
    public double getStockTurnoverRate() {
        List<InventoryUsage> recentUsage = inventoryUsageRepository.findAllByOrderByUsedDateDesc();

        LocalDateTime since = LocalDateTime.now().minusDays(365);
        long unitsUsed = recentUsage.stream()
                .filter(u -> u.getUsedDate() != null && u.getUsedDate().isAfter(since))
                .mapToLong(u -> u.getQuantityUsed() == null ? 0 : u.getQuantityUsed())
                .sum();

        long unitsOnHand = partRepository.findAll().stream()
                .mapToLong(p -> p.getCurrentStock() == null ? 0 : p.getCurrentStock())
                .sum();

        if (unitsOnHand == 0 || unitsUsed == 0) {
            return 0.0;
        }

        return BigDecimal.valueOf(unitsUsed)
                .divide(BigDecimal.valueOf(unitsOnHand), 2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    /**
     * Real usage history for a part over the given trailing window. Empty when no
     * technician has logged consumption of this part yet — callers should treat an
     * empty result as "no real data available" rather than falling back to a guess.
     */
    public List<InventoryUsageResponse> getUsageForPart(Long partId, int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        return inventoryUsageRepository.findByPartIdSince(partId, since)
                .stream()
                .map(inventoryUsageMapper::toResponse)
                .toList();
    }

    /**
     * Real usage across every part over the trailing window, in one call — lets the
     * frontend compute a per-part consumption rate without an HTTP round trip per part.
     */
    public List<InventoryUsageResponse> getAllUsage(int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        return inventoryUsageRepository.findAllSince(since)
                .stream()
                .map(inventoryUsageMapper::toResponse)
                .toList();
    }

    public List<ReorderSummaryResponse> getCriticalReorders(int limit) {
        return reorderRepository.findByStatusOrderByCreatedDateDesc(ReorderStatus.REQUESTED)
            .stream()
            .limit(limit)
            .map(r -> ReorderSummaryResponse.builder()
                .id(r.getId())
                .partName(r.getPart().getName())
                .quantity(r.getQuantity())
                .status(r.getStatus().toString())
                .requestedDate(r.getCreatedDate().toString())
                .build())
            .toList();
    }
}
