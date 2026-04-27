package com.yourpackage.business.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompareRequestDTO {

    @NotNull(message = "machineId is required")
    private Long machineId;

    @NotNull(message = "actionId is required")
    private Long actionId;

    @NotNull(message = "estimatedFailureDowntimeHours is required")
    @Positive(message = "estimatedFailureDowntimeHours must be positive")
    private Double estimatedFailureDowntimeHours;
}
