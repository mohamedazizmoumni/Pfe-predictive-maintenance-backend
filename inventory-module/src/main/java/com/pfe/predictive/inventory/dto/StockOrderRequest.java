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
public class StockOrderRequest implements Serializable {
    private Long reorderRequestId;
    private Long supplierId;
    private String supplierPurchaseOrder;
    private String expectedDeliveryDate;
    private String notes;
}
