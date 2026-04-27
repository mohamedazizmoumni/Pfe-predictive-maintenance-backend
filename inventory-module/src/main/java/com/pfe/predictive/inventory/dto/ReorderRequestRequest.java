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
public class ReorderRequestRequest implements Serializable {
    private Long partId;
    private Integer quantity;
    private String reason;
    private String notes;
    private String requestedBy;
}
