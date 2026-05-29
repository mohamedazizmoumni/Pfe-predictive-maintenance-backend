package com.pfe.predictive.websocket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MachineTelemetryDto {
    
    
    private Long machineId;
    private String serialNumber;
    private String name;
    private String status;
    
   
    private Double health;
   
    private Double remainingUsefulLife;
    
    
    private Double riskScore;
    
    private Double bearingWear;
    
    private Double thermalStress;
   
    private Double lubricationLevel;
    
    private Double fatigueIndex;
    
    private Double efficiencyScore;
    
    private Double temperature;
    
    private Double vibration;
   
    private Double powerConsumption;
    
    private Double pressure;
    
    private Double acousticEmission;
    
    private Double current;
    
    private Double voltage;
    
    private Double rotationSpeed;
    
    private Double efficiency;
    
    private Double ambientTemperature;
    
    private Double loadFactor;
    
    private Double operatingSpeed;
    
    private Double operatingHours;
    
    private Boolean isCritical;
    
    private Boolean isDegrading;
    
    private LocalDateTime timestamp;
}
