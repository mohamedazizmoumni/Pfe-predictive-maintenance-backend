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
public class InventoryUsageRequest implements Serializable {
    private Long partId;
    private Integer quantityUsed;
    private Long taskId;
    private String reason;
    private String notes;
}
