package com.pfe.predictive.machine.mapper;

import com.pfe.predictive.machine.dto.*;
import com.pfe.predictive.core.entity.Machine;
import com.pfe.predictive.core.entity.Sensor;
import com.pfe.predictive.core.entity.SensorData;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Machine Module Mapper
 * Converts entities to DTOs and vice versa
 *
 * @author Machine Module
 * @version 1.0
 */
@Component
public class MachineMapper {

    // ==================== MACHINE MAPPINGS ====================

    public MachineResponse toResponse(Machine machine, int sensorCount, int anomalyCount, int health) {
        if (machine == null) return null;

        return MachineResponse.builder()
            .id(machine.getId())
            .serialNumber(machine.getSerialNumber())
            .model(machine.getModel())
            .location(machine.getLocation())
            .manufacturer(machine.getManufacturer())
            .installationYear(machine.getInstallationYear())
            .status(machine.getStatus() != null ? machine.getStatus().toString() : null)
            .installedDate(machine.getInstalledDate())
            .createdDate(machine.getCreatedDate())
            .lastModifiedDate(machine.getLastModifiedDate())
            .sensorCount(sensorCount)
            .anomalyCount(anomalyCount)
            .health(health)
            .build();
    }

    public MachineDto toDto(Machine machine) {
        if (machine == null) return null;

        return MachineDto.builder()
            .id(machine.getId())
            .serialNumber(machine.getSerialNumber())
            .model(machine.getModel())
            .location(machine.getLocation())
            .status(machine.getStatus() != null ? machine.getStatus().toString() : null)
            .build();
    }

    public List<MachineDto> toDtoList(List<Machine> machines) {
        if (machines == null) return new ArrayList<>();
        return machines.stream().map(this::toDto).toList();
    }

    public List<MachineResponse> toResponseList(List<Machine> machines, List<Integer> sensorCounts, List<Integer> anomalyCounts, List<Integer> healths) {
        if (machines == null) return new ArrayList<>();

        List<MachineResponse> responses = new ArrayList<>();
        for (int i = 0; i < machines.size(); i++) {
            responses.add(toResponse(machines.get(i), sensorCounts.get(i), anomalyCounts.get(i), healths.get(i)));
        }
        return responses;
    }

    // ==================== SENSOR MAPPINGS ====================

    public SensorResponse toResponse(Sensor sensor, long dataPointCount, long anomalyCount, int health, SensorDataResponse latestReading) {
        if (sensor == null) return null;

        return SensorResponse.builder()
            .id(sensor.getId())
            .machineId(sensor.getMachineId())
            .sensorType(sensor.getSensorType())
            .unit(sensor.getUnit())
            .minThreshold(sensor.getMinThreshold())
            .maxThreshold(sensor.getMaxThreshold())
            .createdDate(sensor.getCreatedDate())
            .lastModifiedDate(sensor.getLastModifiedDate())
            .dataPointCount(dataPointCount)
            .anomalyCount(anomalyCount)
            .healthPercentage(health)
            .latestReading(latestReading)
            .build();
    }

    public SensorDto toDto(Sensor sensor, int health) {
        if (sensor == null) return null;

        return SensorDto.builder()
            .id(sensor.getId())
            .sensorType(sensor.getSensorType())
            .unit(sensor.getUnit())
            .health(health)
            .build();
    }

    public List<SensorDto> toDtoList(List<Sensor> sensors) {
        if (sensors == null) return new ArrayList<>();
        List<SensorDto> dtos = new ArrayList<>();
        for (Sensor sensor : sensors) {
            dtos.add(toDto(sensor, 100)); // Default health
        }
        return dtos;
    }

    // ==================== SENSOR DATA MAPPINGS ====================

    public SensorDataResponse toResponse(SensorData sensorData) {
        if (sensorData == null) return null;

        return SensorDataResponse.builder()
            .id(sensorData.getId())
            .sensorId(sensorData.getSensorId())
            .value(sensorData.getValue())
            .unit(sensorData.getUnit())
            .isAnomaly(sensorData.getIsAnomaly())
            .timestamp(sensorData.getTimestamp())
            .notes(sensorData.getNotes())
            .build();
    }

    public List<SensorDataResponse> toResponseList(List<SensorData> dataList) {
        if (dataList == null) return new ArrayList<>();
        return dataList.stream().map(this::toResponse).toList();
    }

    public SensorDataBatchResponse toBatchResponse(Long sensorId, int recordCount, int anomalyCount, Double avgValue, Double minValue, Double maxValue) {
        return SensorDataBatchResponse.builder()
            .sensorId(sensorId)
            .recordCount(recordCount)
            .anomalyCount(anomalyCount)
            .batchTime(java.time.LocalDateTime.now())
            .averageValue(avgValue)
            .minValue(minValue)
            .maxValue(maxValue)
            .build();
    }

    // ==================== PAGE MAPPINGS ====================

    public org.springframework.data.domain.Page<MachineDto> toDtoPage(Page<Machine> page) {
        return page.map(this::toDto);
    }

    public org.springframework.data.domain.Page<SensorDataResponse> toResponsePage(Page<SensorData> page) {
        return page.map(this::toResponse);
    }
}
