package com.pfe.predictive.portal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupportTicketRequest {

    @NotNull(message = "Machine is required")
    private Long machineId;

    @NotBlank(message = "Subject is required")
    private String subject;

    private String description;
}
