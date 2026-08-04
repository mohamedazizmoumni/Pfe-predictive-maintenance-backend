package com.pfe.predictive.inventory.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * Payload the ML service sends when it predicts a part will reach a
 * critical stock level. Carries the AI's recommendation only — creating a
 * ReorderRequest/StockOrder from this data is a manual Stock Manager action.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ShortageForecastRequest implements Serializable {

    @NotNull
    @Min(1)
    private Integer recommendedQuantity;

    @NotBlank
    @Size(max = 1000)
    private String reason;

    @DecimalMin(value = "0.0")
    @DecimalMax(value = "1.0")
    private Double confidence;

    private LocalDate predictedStockoutDate;
}
