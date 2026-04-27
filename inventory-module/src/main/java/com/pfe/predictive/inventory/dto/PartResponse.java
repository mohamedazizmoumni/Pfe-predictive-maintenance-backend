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
public class PartResponse implements Serializable {
    private Long id;
    private String name;
    private String description;
    private String partNumber;
    private String category;
    private BigDecimal cost;
    private Integer currentStock;
    private Integer minimumStock;
    private Integer reorderQuantity;
    private String unit;
    private String supplier;
    private String status;
    private String notes;
    private String createdDate;
    private String lastModifiedDate;
}
