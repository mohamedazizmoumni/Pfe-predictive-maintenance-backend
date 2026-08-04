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
public class PortalMachineSummaryDto {
    private Long machineId;
    private String name;
    private String serialNumber;
    private String location;
    /** Plain-language status, e.g. "Running normally", "Under maintenance", "Needs attention" — never a raw enum. */
    private String statusLabel;
    /** 0-100, derived from the machine's current risk score. */
    private Integer healthScore;
    private LocalDateTime lastMaintenanceDate;
    private LocalDateTime nextMaintenanceDate;
}
