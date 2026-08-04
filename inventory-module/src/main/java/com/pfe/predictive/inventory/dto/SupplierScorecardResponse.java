package com.pfe.predictive.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierScorecardResponse {
    private Long supplierId;
    private String supplierName;
    private long totalOrders;
    private long deliveredOrders;
    private BigDecimal totalSpend;
    /** Average actual days from order to delivery, across delivered orders with parseable dates. Null if none could be computed. */
    private Double avgActualLeadTimeDays;
    /** Share of delivered orders that arrived at or before the supplier's quoted leadTimeDays. Null if leadTimeDays isn't set or no orders could be compared. */
    private Double onTimeDeliveryRate;
}
