package com.pfe.predictive.inventory.service;

import com.pfe.predictive.inventory.dto.DemandForecastResponse;
import com.pfe.predictive.inventory.entity.InventoryUsage;
import com.pfe.predictive.inventory.entity.Part;
import com.pfe.predictive.inventory.repository.InventoryUsageRepository;
import com.pfe.predictive.inventory.repository.PartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gives inventory.low-stock-threshold-percentage's sibling concept — proactive
 * reordering — a forward-looking basis: projected days-until-stockout from
 * real consumption history (InventoryUsage), not just the current snapshot
 * AutoReorderService already reacts to. Stock Manager extended with
 * Procurement Officer's "demand forecast" responsibility (2026-08-01 scope
 * reorientation).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DemandForecastService {

    private static final int LOOKBACK_DAYS = 90;

    private final InventoryUsageRepository inventoryUsageRepository;
    private final PartRepository partRepository;

    public List<DemandForecastResponse> getForecast() {
        LocalDateTime since = LocalDateTime.now().minusDays(LOOKBACK_DAYS);
        List<InventoryUsage> usages = inventoryUsageRepository.findAllSince(since);

        Map<Long, Integer> netUsageByPart = new HashMap<>();
        for (InventoryUsage usage : usages) {
            if (usage.getPart() == null) continue;
            netUsageByPart.merge(usage.getPart().getId(), usage.getQuantityUsed(), Integer::sum);
        }

        List<DemandForecastResponse> forecasts = new ArrayList<>();
        for (Part part : partRepository.findAll()) {
            Integer netUsage = netUsageByPart.get(part.getId());
            Double avgDailyConsumption = (netUsage != null && netUsage > 0)
                    ? netUsage / (double) LOOKBACK_DAYS
                    : null;

            Double daysUntilStockout = (avgDailyConsumption != null && avgDailyConsumption > 0 && part.getCurrentStock() != null)
                    ? part.getCurrentStock() / avgDailyConsumption
                    : null;

            boolean belowMinimum = part.getCurrentStock() != null && part.getMinimumStock() != null
                    && part.getCurrentStock() <= part.getMinimumStock();

            forecasts.add(DemandForecastResponse.builder()
                    .partId(part.getId())
                    .partName(part.getName())
                    .currentStock(part.getCurrentStock())
                    .minimumStock(part.getMinimumStock())
                    .avgDailyConsumption(avgDailyConsumption)
                    .projectedDaysUntilStockout(daysUntilStockout)
                    .belowMinimum(belowMinimum)
                    .build());
        }

        // Soonest projected stockout first; parts with no consumption trend
        // (nothing to project) sort last rather than cluttering the top.
        forecasts.sort(Comparator.comparing(
                DemandForecastResponse::getProjectedDaysUntilStockout,
                Comparator.nullsLast(Comparator.naturalOrder())));
        return forecasts;
    }
}
