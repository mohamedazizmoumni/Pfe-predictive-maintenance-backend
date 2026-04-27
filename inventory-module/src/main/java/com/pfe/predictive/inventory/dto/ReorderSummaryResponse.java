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
public class ReorderSummaryResponse implements Serializable {
    private Long id;
    private String partName;
    private Integer quantity;
    private Integer estimatedCost;
    private String status;
    private String requestedDate;
}
