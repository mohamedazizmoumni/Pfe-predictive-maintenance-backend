package com.pfe.predictive.core.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "sensor_telemetry", indexes = {
    @Index(name = "idx_machine_timestamp", columnList = "machine_id,timestamp DESC"),
    @Index(name = "idx_timestamp", columnList = "timestamp DESC")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SensorTelemetry {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "machine_id", nullable = false)
    private Long machineId;
    
    @Column(nullable = false)
    private LocalDateTime timestamp;
    
    
    @Column(name = "setting1")
    private Double setting1;
  
    @Column(name = "setting2")
    private Double setting2;
    
    @Column(name = "setting3")
    private Double setting3;
  
    @Column(name = "sensor1")
    private Double sensor1;
   
    @Column(name = "sensor2")
    private Double sensor2;
    
    @Column(name = "sensor3")
    private Double sensor3;
    
    @Column(name = "sensor4")
    private Double sensor4;
   
    @Column(name = "sensor5")
    private Double sensor5;
    
    @Column(name = "sensor6")
    private Double sensor6;
    
    @Column(name = "sensor7")
    private Double sensor7;
    
    @Column(name = "sensor8")
    private Double sensor8;
    
    @Column(name = "sensor9")
    private Double sensor9;
    
    @Column(name = "sensor10")
    private Double sensor10;
    
    @Column(name = "sensor11")
    private Double sensor11;
    
    @Column(name = "sensor12")
    private Double sensor12;
    
    @Column(name = "sensor13")
    private Double sensor13;
    
    @Column(name = "sensor14")
    private Double sensor14;
    
    @Column(name = "sensor15")
    private Double sensor15;
    
    @Column(name = "sensor16")
    private Double sensor16;
    
    @Column(name = "sensor17")
    private Double sensor17;
    
    @Column(name = "sensor18")
    private Double sensor18;
   
    @Column(name = "sensor19")
    private Double sensor19;
    
    @Column(name = "sensor20")
    private Double sensor20;
    
    @Column(name = "sensor21")
    private Double sensor21;
    
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
