package com.pfe.predictive.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryStatsResponse implements Serializable {
    private long totalPartsTracked;
    private long lowStockPartsCount;
    private long outOfStockPartsCount;
    private long pendingOrdersCount;
    private Integer totalInventoryValue;
    private double turnoverRate;
    private String lastUpdated;
}
