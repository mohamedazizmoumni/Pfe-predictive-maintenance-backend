package com.pfe.predictive.core.entity.finance;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Embeddable
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RapportPart {

    // Nullable: only set when the technician picked this line from an active
    // PartReservation for the job (see task-completion-modal's "Use Reserved
    // Part" action) rather than typing a free-text part name. Lets
    // MaintenanceRapportService.recordPartUsage() consume the matching
    // reservation instead of doing a second, independent stock decrement.
    // Null means "free-text entry, name-match against the Part catalog" -
    // the original, still-supported behavior.
    @Column(name = "part_id")
    private Long partId;

    @Column(name = "part_name", nullable = false, length = 255)
    private String partName;

    @Column(name = "part_code", length = 100)
    private String partCode;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "unit_cost", nullable = false, precision = 19, scale = 2)
    private BigDecimal unitCost;

    @Column(name = "total_cost", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalCost;

    @Column(length = 255)
    private String supplier;
}
