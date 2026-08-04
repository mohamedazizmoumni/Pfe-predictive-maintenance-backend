package com.pfe.predictive.portal.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortalMessageRequest {

    @NotBlank(message = "Message body is required")
    private String body;
}
