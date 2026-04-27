package com.pfe.predictive.machine.entity;

import lombok.*;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "machines")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Machine {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String serialNumber;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 100)
    private String model;

    @Column(length = 255)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MachineStatus status; // OPERATIONAL, MAINTENANCE, FAULTY, DECOMMISSIONED

    @Column
    private LocalDateTime installationDate;

    @Column
    private LocalDateTime lastMaintenanceDate;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @UpdateTimestamp
    @Column
    private LocalDateTime lastModifiedDate;

    @Version
    private Long version;
}

@Entity
@Table(name = "sensors")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Sensor {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "machine_id", nullable = false)
    private Machine machine;

    @Column(nullable = false, length = 100)
    private String sensorType; // TEMPERATURE, VIBRATION, PRESSURE, etc.

    @Column(nullable = false, unique = true, length = 100)
    private String sensorId;

    @Column(length = 1000)
    private String location;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @Version
    private Long version;
}

@Entity
@Table(name = "sensor_data")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SensorData {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sensor_id", nullable = false)
    private Sensor sensor;

    @Column
    private Double value;

    @Column(length = 50)
    private String unit;

    @Column
    private LocalDateTime recordedDate;

    @Version
    private Long version;
}

public enum MachineStatus {
    OPERATIONAL,
    MAINTENANCE,
    FAULTY,
    DECOMMISSIONED
}
