package com.pfe.predictive.inventory.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
@JsonIgnoreProperties(ignoreUnknown = true)
public class PartUpdateRequest implements Serializable {
    @JsonAlias({"partName"})
    private String name;

    @JsonAlias({"partDescription"})
    private String description;

    private String category;

    private String subCategory;

    @JsonAlias({"unitCost", "price"})
    private BigDecimal cost;

    @JsonAlias({"minStock", "minimum_stock"})
    private Integer minimumStock;

    @JsonAlias({"reorderQty", "reorder_quantity"})
    private Integer reorderQuantity;

    @JsonAlias({"supplierName"})
    private String supplier;

    @JsonAlias({"stock", "current_stock", "currentStock"})
    private Integer currentStock;

    @JsonAlias({"sku", "part_code"})
    private String partNumber;

    private String unit;

    private String notes;
}
