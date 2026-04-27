package com.pfe.predictive.maintenancecost.entity;

import com.pfe.predictive.maintenancecost.enums.CriticalityLevel;
import com.pfe.predictive.maintenancecost.enums.MachineStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity(name = "MaintenanceCostMachine")
@Table(name = "maintenance_cost_machines")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Machine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "location", nullable = false, length = 255)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private MachineStatus status;

    @Column(name = "hourly_production_value", nullable = false, precision = 19, scale = 4)
    private BigDecimal hourlyProductionValue;

    @Column(name = "replacement_cost", nullable = false, precision = 19, scale = 4)
    private BigDecimal replacementCost;

    @Enumerated(EnumType.STRING)
    @Column(name = "criticality_level", nullable = false, length = 20)
    private CriticalityLevel criticalityLevel;

    @Column(name = "age", nullable = false)
    private Integer age;
}
