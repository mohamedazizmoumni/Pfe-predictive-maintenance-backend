package com.pfe.predictive.core.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "machine_simulation_state")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MachineSimulationState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long machineId;

    @Column(nullable = false)
    @Builder.Default
    private Double health = 100.0;  

    @Column(nullable = false)
    @Builder.Default
    private Double bearingWear = 0.0;  

    @Column(nullable = false)
    @Builder.Default
    private Double thermalStress = 0.0;  

    @Column(nullable = false)
    @Builder.Default
    private Double lubricationLevel = 100.0;  

    @Column(nullable = false)
    @Builder.Default
    private Double fatigueIndex = 0.0; 

    @Column(nullable = false)
    @Builder.Default
    private Double efficiencyScore = 100.0;  

    
    @Column(nullable = false)
    @Builder.Default
    private Long operatingHours = 0L;

    @Column(nullable = false)
    @Builder.Default
    private Integer cyclesSinceLastMaintenance = 0;

    @Column
    private LocalDateTime lastMaintenanceDate;

    
    @Column(nullable = false)
    @Builder.Default
    private Double ambientTemperature = 70.0;  

    @Column(nullable = false)
    @Builder.Default
    private Double loadFactor = 0.5;  

    @Column(nullable = false)
    @Builder.Default
    private Double operatingSpeed = 50.0;  

    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column
    private LocalDateTime updatedAt;

    public Double calculateOverallHealth() {
        return ((100.0 - bearingWear) * 0.25)
                + ((100.0 - thermalStress) * 0.20)
                + (lubricationLevel * 0.20)
                + ((100.0 - fatigueIndex) * 0.20)
                + (efficiencyScore * 0.15);
    }

    public void performMaintenance(String type) {
        switch (type.toUpperCase()) {
            case "PREVENTIVE":
                bearingWear *= 0.5;
                lubricationLevel = Math.min(100.0, lubricationLevel + 30.0);
                thermalStress *= 0.7;
                break;
            case "CORRECTIVE":
                bearingWear = 0.0;
                lubricationLevel = 100.0;
                thermalStress = 0.0;
                fatigueIndex *= 0.3;
                break;
            case "PREDICTIVE":
                if (bearingWear > 60) bearingWear *= 0.2;
                if (lubricationLevel < 40) lubricationLevel = 60.0;
                if (thermalStress > 60) thermalStress *= 0.3;
                break;
        }
        cyclesSinceLastMaintenance = 0;
        lastMaintenanceDate = LocalDateTime.now();
        health = calculateOverallHealth();
    }

    public boolean isCriticalFailure() {
        return health < 30 || bearingWear > 80 || lubricationLevel < 10 || fatigueIndex > 85;
    }

   
    public Double estimateRUL() {
        if (health > 80) return 500.0;
        if (health > 60) return 300.0;
        if (health > 40) return 150.0;
        if (health > 20) return 50.0;
        return 10.0;
    }

    /**
     * Evolve the machine state over time (simulate degradation).
     * @param timeStepMinutes Time step in minutes
     */
    public void evolveState(double timeStepMinutes) {
        // Simulate degradation based on operating conditions
        double timeStepHours = timeStepMinutes / 60.0;
        
        // Bearing wear increases with load and speed
        bearingWear += (loadFactor * operatingSpeed / 1000.0) * timeStepHours * 0.1;
        
        // Thermal stress increases with temperature and load
        thermalStress += ((ambientTemperature - 70.0) / 50.0 + loadFactor) * timeStepHours * 0.05;
        
        // Lubrication degrades with temperature and speed
        lubricationLevel -= (ambientTemperature / 100.0 + operatingSpeed / 1000.0) * timeStepHours * 0.02;
        
        // Fatigue accumulates with cycles
        fatigueIndex += (loadFactor * loadFactor) * timeStepHours * 0.08;
        
        // Efficiency decreases with degradation
        efficiencyScore = 100.0 - (bearingWear * 0.2 + thermalStress * 0.2 + (100.0 - lubricationLevel) * 0.2 + fatigueIndex * 0.2);
        
        // Clamp values to 0-100
        bearingWear = Math.max(0.0, Math.min(100.0, bearingWear));
        thermalStress = Math.max(0.0, Math.min(100.0, thermalStress));
        lubricationLevel = Math.max(0.0, Math.min(100.0, lubricationLevel));
        fatigueIndex = Math.max(0.0, Math.min(100.0, fatigueIndex));
        efficiencyScore = Math.max(0.0, Math.min(100.0, efficiencyScore));
        
        // Update overall health
        health = calculateOverallHealth();
        
        // Update operating hours
        operatingHours += (long) timeStepHours;
        cyclesSinceLastMaintenance += (int) (loadFactor * 10);
    }

    /**
     * Get cooling efficiency (derived from thermal stress and lubrication).
     */
    public Double getCoolingEfficiency() {
        return 100.0 - (thermalStress * 0.5 + (100.0 - lubricationLevel) * 0.5);
    }

    /**
     * Get electrical resistance (derived from bearing wear and efficiency).
     */
    public Double getElectricalResistance() {
        return (bearingWear * 0.3 + (100.0 - efficiencyScore) * 0.7) / 100.0;
    }

    public static MachineSimulationState initializeHealthy(Long machineId) {
        return MachineSimulationState.builder()
                .machineId(machineId)
                .health(100.0)
                .bearingWear(0.0)
                .thermalStress(0.0)
                .lubricationLevel(100.0)
                .fatigueIndex(0.0)
                .efficiencyScore(100.0)
                .operatingHours(0L)
                .cyclesSinceLastMaintenance(0)
                .lastMaintenanceDate(LocalDateTime.now())
                .ambientTemperature(70.0)
                .loadFactor(0.5)
                .operatingSpeed(50.0)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public static MachineSimulationState initializePartiallyDegraded(Long machineId) {
        return MachineSimulationState.builder()
                .machineId(machineId)
                .health(60.0)
                .bearingWear(40.0)
                .thermalStress(35.0)
                .lubricationLevel(60.0)
                .fatigueIndex(30.0)
                .efficiencyScore(70.0)
                .operatingHours(5000L)
                .cyclesSinceLastMaintenance(1000)
                .lastMaintenanceDate(LocalDateTime.now().minusMonths(6))
                .ambientTemperature(75.0)
                .loadFactor(0.7)
                .operatingSpeed(60.0)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
