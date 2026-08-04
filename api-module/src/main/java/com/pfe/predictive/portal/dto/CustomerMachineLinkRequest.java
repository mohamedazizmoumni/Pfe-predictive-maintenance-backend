package com.pfe.predictive.portal.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerMachineLinkRequest {

    @NotNull(message = "Customer user ID is required")
    private Long userId;

    @NotNull(message = "Machine ID is required")
    private Long machineId;
}
