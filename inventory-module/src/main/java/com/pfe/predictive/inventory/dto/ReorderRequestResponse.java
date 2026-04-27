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
public class ReorderRequestResponse implements Serializable {
    private Long id;
    private Long partId;
    private String partName;
    private Integer quantity;
    private java.math.BigDecimal approximateCost;
    private String reason;
    private String status;
    private String requestedBy;
    private String requestedDate;
    private String approvedBy;
    private String approvedDate;
    private String notes;
}
