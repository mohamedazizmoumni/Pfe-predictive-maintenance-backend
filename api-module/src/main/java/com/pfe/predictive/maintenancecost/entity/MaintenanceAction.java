package com.pfe.predictive.maintenancecost.entity;

import com.pfe.predictive.maintenancecost.enums.MaintenanceActionStatus;
import com.pfe.predictive.maintenancecost.enums.MaintenanceActionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "maintenance_actions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "machine_id", nullable = false)
    private Machine machine;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MaintenanceActionType type;

    @Column(nullable = false)
    private Double estimatedDurationHours;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal laborCostPerHour;

    @Builder.Default
    @ManyToMany
    @JoinTable(
            name = "maintenance_action_parts",
            joinColumns = @JoinColumn(name = "maintenance_action_id"),
            inverseJoinColumns = @JoinColumn(name = "maintenance_part_id")
    )
    private Set<MaintenancePart> parts = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MaintenanceActionStatus status;

    @Column(nullable = false)
    private LocalDateTime scheduledDate;
}
