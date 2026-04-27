package com.pfe.predictive.inventory.service;

import com.pfe.predictive.inventory.dto.InventoryStatsResponse;
import com.pfe.predictive.inventory.dto.LowStockAlertResponse;
import com.pfe.predictive.inventory.dto.ReorderSummaryResponse;
import com.pfe.predictive.inventory.entity.PartStatus;
import com.pfe.predictive.inventory.entity.ReorderStatus;
import com.pfe.predictive.inventory.entity.StockOrderStatus;
import com.pfe.predictive.inventory.repository.PartRepository;
import com.pfe.predictive.inventory.repository.ReorderRequestRepository;
import com.pfe.predictive.inventory.repository.StockOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryAnalyticsService {

    private final PartRepository partRepository;
    private final ReorderRequestRepository reorderRepository;
    private final StockOrderRepository stockOrderRepository;
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

    public double getStockTurnoverRate() {
        // TODO: compute from historical usage data once available
        return 4.5;
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
