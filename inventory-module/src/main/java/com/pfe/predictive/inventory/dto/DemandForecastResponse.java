package com.pfe.predictive.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DemandForecastResponse {
    private Long partId;
    private String partName;
    private Integer currentStock;
    private Integer minimumStock;
    /** Net units consumed per day, averaged over the lookback window. */
    private Double avgDailyConsumption;
    /** Projected days until stock hits zero at the current consumption rate. Null if consumption is flat/negative. */
    private Double projectedDaysUntilStockout;
    private boolean belowMinimum;
}
