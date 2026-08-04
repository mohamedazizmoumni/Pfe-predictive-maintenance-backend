package com.pfe.predictive.inventory.dto;

import com.pfe.predictive.inventory.entity.PartReservationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartReservationResponse {
    private Long id;
    private Long partId;
    private String partName;
    private Integer quantityReserved;
    private Long maintenanceId;
    private PartReservationStatus status;
    private String reservedBy;
    private LocalDateTime reservedAt;
    private LocalDateTime resolvedAt;
}
