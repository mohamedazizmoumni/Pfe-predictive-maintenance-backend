package com.pfe.predictive.inventory.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class PartReservationRequest {
    @NotNull
    private Long partId;
    @NotNull
    @Positive
    private Integer quantity;
    private Long maintenanceId;
}
