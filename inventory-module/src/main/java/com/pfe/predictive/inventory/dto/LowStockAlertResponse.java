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
public class LowStockAlertResponse implements Serializable {
    private Long partId;
    private String partName;
    private Integer currentStock;
    private Integer minimumStock;
    private Integer reorderQuantity;
    private String status;
}
