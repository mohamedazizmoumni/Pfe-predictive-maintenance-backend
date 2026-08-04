package com.pfe.predictive.core.entity.finance;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Embeddable
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RapportPart {

    @Column(name = "part_name", nullable = false, length = 255)
    private String partName;

    @Column(name = "part_code", length = 100)
    private String partCode;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "unit_cost", nullable = false, precision = 19, scale = 2)
    private BigDecimal unitCost;

    @Column(name = "total_cost", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalCost;

    @Column(length = 255)
    private String supplier;
}
