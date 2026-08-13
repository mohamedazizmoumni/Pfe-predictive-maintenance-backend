package com.pfe.predictive.finance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RapportPartRequest {

    // Set when this line came from an active PartReservation for the job
    // (see RapportPart.partId javadoc) - null for a free-text entry.
    private Long partId;

    @NotBlank(message = "Part name is required")
    private String partName;

    private String partCode;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    private Integer quantity;

    @NotNull(message = "Unit cost is required")
    @Positive(message = "Unit cost must be positive")
    private BigDecimal unitCost;

    private String supplier;
}
