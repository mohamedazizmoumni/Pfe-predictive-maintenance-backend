package com.pfe.predictive.core.entity.finance;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One line of a technician's inspection checklist on a MaintenanceRapport.
 * Mirrors RapportPart's pattern (embeddable collected via @ElementCollection —
 * checklist items have no independent identity outside their rapport).
 */
@Embeddable
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChecklistItem {

    @Column(name = "description", nullable = false, length = 500)
    private String description;

    @Column(name = "passed", nullable = false)
    private boolean passed;

    @Column(name = "notes", length = 1000)
    private String notes;
}
