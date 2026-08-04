package com.pfe.predictive.portal.dto;

import com.pfe.predictive.core.entity.portal.SupportTicketStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupportTicketUpdateRequest {

    @NotNull(message = "Status is required")
    private SupportTicketStatus status;

    private String resolutionNotes;
}
