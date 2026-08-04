package com.pfe.predictive.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockOrderResponse implements Serializable {
    private Long id;
    private Long reorderRequestId;
    private Long partId;
    private String partName;
    private Integer quantity;
    private BigDecimal cost;
    private String status;
    private Long supplierId;
    private String supplierName;
    private String supplierPurchaseOrder;
    private String orderedDate;
    private String expectedDeliveryDate;
    private String deliveredDate;
    private String orderedBy;
    private String notes;
}
