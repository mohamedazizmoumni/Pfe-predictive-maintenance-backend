package com.pfe.predictive.portal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortalMaintenanceHistoryDto {
    private Long id;
    private String type;
    private String description;
    private String statusLabel;
    private LocalDateTime scheduledDate;
    private LocalDateTime completedDate;
}
