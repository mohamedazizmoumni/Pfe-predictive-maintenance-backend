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
public class InventoryUsageResponse implements Serializable {
    private Long id;
    private Long partId;
    private String partName;
    private Integer quantityUsed;
    private Long taskId;
    private String reason;
    private String usedBy;
    private String usedDate;
    private String notes;
}
