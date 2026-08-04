package com.pfe.predictive.portal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarrantyResponse {
    private Long id;
    private Long machineId;
    private String machineName;
    private String provider;
    private LocalDate startDate;
    private LocalDate endDate;
    private String terms;
    private boolean active;
}
