package com.pfe.predictive.machine.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Machine Module Request/Response DTOs
 * Used for API request validation and response serialization
 *
 * @author Machine Module
 * @version 1.0
 */

// ==================== MACHINE DTOS ====================

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MachineRequest {
    @NotBlank(message = "Serial number is required")
    private String serialNumber;

    @NotBlank(message = "Model is required")
    private String model;

    @NotBlank(message = "Location is required")
    private String location;

    private String manufacturer;

    @Min(1900)
    @Max(2100)
    private Integer installationYear;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MachineResponse {
    private Long id;

    private String serialNumber;

    private String model;

    private String location;

    private String manufacturer;

    private Integer installationYear;

    private String status;

    private LocalDateTime installedDate;

    private LocalDateTime createdDate;

    private LocalDateTime lastModifiedDate;

    private int sensorCount;

    private int anomalyCount;

    private int health; // percentage
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MachineDto {
    private Long id;
    private String serialNumber;
    private String model;
    private String location;
    private String status;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MachineStatsResponse {
    private long totalMachines;
    private long operationalMachines;
    private long maintenanceMachines;
    private long faultyMachines;
    private long inactiveeMachines;
    private double averageHealth;
}

// ==================== SENSOR DTOS ====================

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SensorRequest {
    @NotBlank(message = "Sensor type is required")
    private String sensorType;

    @NotBlank(message = "Unit is required")
    private String unit;

    private Double minThreshold;

    private Double maxThreshold;

    private Integer batchSize; // for batch operations
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SensorResponse {
    private Long id;

    private Long machineId;

    private String sensorType;

    private String unit;

    private Double minThreshold;

    private Double maxThreshold;

    private LocalDateTime createdDate;

    private LocalDateTime lastModifiedDate;

    private long dataPointCount;

    private long anomalyCount;

    private int healthPercentage;

    private SensorDataResponse latestReading;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SensorDto {
    private Long id;
    private String sensorType;
    private String unit;
    private int health;
}

// ==================== SENSOR DATA DTOS ====================

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SensorDataRequest {
    @NotNull(message = "Value is required")
    @DecimalMin("0.0")
    private Double value;

    private Boolean isAnomaly;

    private Integer batchSize; // for batch operations

    private String notes;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SensorDataResponse {
    private Long id;

    private Long sensorId;

    private Double value;

    private String unit;

    private Boolean isAnomaly;

    private LocalDateTime timestamp;

    private String notes;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SensorDataBatchResponse {
    private Long sensorId;
    private int recordCount;
    private int anomalyCount;
    private LocalDateTime batchTime;
    private Double averageValue;
    private Double minValue;
    private Double maxValue;
}

// ==================== COMPOSITE DTOS ====================

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MachineDetailResponse {
    private MachineResponse machine;
    private List<SensorResponse> sensors;
    private int anomalyCount;
    private LocalDateTime lastDataPoint;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SensorDetailResponse {
    private SensorResponse sensor;
    private List<SensorDataResponse> recentReadings;
    private Map<String, Object> statistics;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MachineHealthReport {
    private Long machineId;
    private String serialNumber;
    private int overallHealth;
    private List<SensorHealthDto> sensorHealth;
    private List<SensorDataResponse> recentAnomalies;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SensorHealthDto {
    private Long sensorId;
    private String sensorType;
    private int health;
    private long anomalyCount;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MachineStatusChangeRequest {
    @NotBlank(message = "Status is required")
    private String status;

    private String reason;
}
