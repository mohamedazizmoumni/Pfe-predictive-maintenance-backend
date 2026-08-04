package com.pfe.predictive.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RapportPartResponse {

    /** Positional index within the rapport (parts have no independent identity). */
    private Long id;
    private String partName;
    private String partCode;
    private Integer quantity;
    private BigDecimal unitCost;
    private BigDecimal totalCost;
    private String supplier;
}
