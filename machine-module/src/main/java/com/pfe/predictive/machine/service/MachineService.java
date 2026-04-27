package com.pfe.predictive.machine.service;

import com.pfe.predictive.machine.dto.MachineRequest;
import com.pfe.predictive.machine.dto.SensorDataRequest;
import com.pfe.predictive.machine.dto.SensorRequest;
import com.pfe.predictive.core.entity.Machine;
import com.pfe.predictive.core.entity.MachineStatus;
import com.pfe.predictive.core.entity.Sensor;
import com.pfe.predictive.core.entity.SensorData;
import com.pfe.predictive.data.repository.MachineRepository;
import com.pfe.predictive.data.repository.SensorDataRepository;
import com.pfe.predictive.data.repository.SensorRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Machine Service
 * Handles write operations for machines, sensors, and sensor data
 * All operations are transactional with proper error handling
 *
 * @author Machine Module
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MachineService {

    private final MachineRepository machineRepository;
    private final SensorRepository sensorRepository;
    private final SensorDataRepository sensorDataRepository;

    // ==================== MACHINE OPERATIONS ====================

    /**
     * Create new machine
     * @param request Machine creation request
     * @param createdBy User creating the machine
     * @return Created machine
     */
    public Machine createMachine(MachineRequest request, String createdBy) {
        log.info("Creating new machine: {}", request.getSerialNumber());

        if (machineRepository.existsBySerialNumber(request.getSerialNumber())) {
            throw new IllegalArgumentException("Machine with serial number already exists: " + request.getSerialNumber());
        }

        Machine machine = Machine.builder()
            .serialNumber(request.getSerialNumber())
            .model(request.getModel())
            .location(request.getLocation())
            .manufacturer(request.getManufacturer())
            .installationYear(request.getInstallationYear())
            .installedDate(LocalDateTime.now())
            .status(MachineStatus.OPERATIONAL)
            .createdBy(createdBy)
            .build();

        Machine saved = machineRepository.save(machine);
        log.debug("Machine created successfully with ID: {}", saved.getId());
        return saved;
    }

    /**
     * Update machine details
     * @param machineId Machine to update
     * @param request Update request
     * @return Updated machine
     */
    public Machine updateMachine(Long machineId, MachineRequest request) {
        log.info("Updating machine: {}", machineId);

        Machine machine = machineRepository.findById(machineId)
            .orElseThrow(() -> new EntityNotFoundException("Machine not found: " + machineId));

        if (request.getLocation() != null) {
            machine.setLocation(request.getLocation());
        }
        if (request.getManufacturer() != null) {
            machine.setManufacturer(request.getManufacturer());
        }

        Machine updated = machineRepository.save(machine);
        log.debug("Machine updated: {}", machineId);
        return updated;
    }

    /**
     * Change machine status
     * @param machineId Machine to update
     * @param status New status
     * @return Updated machine
     */
    public Machine updateMachineStatus(Long machineId, MachineStatus status) {
        log.info("Changing machine status - ID: {}, Status: {}", machineId, status);

        Machine machine = machineRepository.findById(machineId)
            .orElseThrow(() -> new EntityNotFoundException("Machine not found: " + machineId));

        machine.setStatus(status);
        return machineRepository.save(machine);
    }

    /**
     * Mark machine for maintenance
     * @param machineId Machine ID
     * @return Updated machine
     */
    public Machine markForMaintenance(Long machineId) {
        return updateMachineStatus(machineId, MachineStatus.MAINTENANCE);
    }

    /**
     * Mark machine as faulty
     * @param machineId Machine ID
     * @param reason Reason for fault
     * @return Updated machine
     */
    public Machine markAsFaulty(Long machineId, String reason) {
        Machine machine = machineRepository.findById(machineId)
            .orElseThrow(() -> new EntityNotFoundException("Machine not found: " + machineId));

        machine.setStatus(MachineStatus.FAULTY);
        log.warn("Machine marked as faulty - ID: {}, Reason: {}", machineId, reason);
        return machineRepository.save(machine);
    }

    // ==================== SENSOR OPERATIONS ====================

    /**
     * Add sensor to machine
     * @param machineId Machine ID
     * @param request Sensor request
     * @return Created sensor
     */
    public Sensor addSensorToMachine(Long machineId, SensorRequest request) {
        log.info("Adding sensor to machine: {}", machineId);

        Machine machine = machineRepository.findById(machineId)
            .orElseThrow(() -> new EntityNotFoundException("Machine not found: " + machineId));

        if (sensorRepository.findByMachineIdAndSensorType(machineId, request.getSensorType()).isPresent()) {
            throw new IllegalArgumentException("Sensor type already exists for this machine: " + request.getSensorType());
        }

        Sensor sensor = Sensor.builder()
            .machineId(machineId)
            .sensorType(request.getSensorType())
            .unit(request.getUnit())
            .minThreshold(request.getMinThreshold())
            .maxThreshold(request.getMaxThreshold())
            .createdBy("system")
            .build();

        Sensor saved = sensorRepository.save(sensor);
        log.debug("Sensor added to machine - MachineID: {}, SensorType: {}", machineId, request.getSensorType());
        return saved;
    }

    /**
     * Update sensor configuration
     * @param sensorId Sensor ID
     * @param request Update request
     * @return Updated sensor
     */
    public Sensor updateSensor(Long sensorId, SensorRequest request) {
        log.info("Updating sensor: {}", sensorId);

        Sensor sensor = sensorRepository.findById(sensorId)
            .orElseThrow(() -> new EntityNotFoundException("Sensor not found: " + sensorId));

        if (request.getMinThreshold() != null) {
            sensor.setMinThreshold(request.getMinThreshold());
        }
        if (request.getMaxThreshold() != null) {
            sensor.setMaxThreshold(request.getMaxThreshold());
        }

        return sensorRepository.save(sensor);
    }

    /**
     * Remove sensor from machine
     * @param sensorId Sensor ID
     */
    public void removeSensor(Long sensorId) {
        log.info("Removing sensor: {}", sensorId);

        Sensor sensor = sensorRepository.findById(sensorId)
            .orElseThrow(() -> new EntityNotFoundException("Sensor not found: " + sensorId));

        sensorRepository.delete(sensor);
        log.debug("Sensor removed: {}", sensorId);
    }

    // ==================== SENSOR DATA OPERATIONS ====================

    /**
     * Record sensor reading
     * @param sensorId Sensor ID
     * @param request Sensor data request
     * @return Created sensor data
     */
    public SensorData recordSensorReading(Long sensorId, SensorDataRequest request) {
        log.debug("Recording sensor reading - SensorID: {}, Value: {}", sensorId, request.getValue());

        Sensor sensor = sensorRepository.findById(sensorId)
            .orElseThrow(() -> new EntityNotFoundException("Sensor not found: " + sensorId));

        // Detect anomaly based on thresholds
        boolean isAnomaly = false;
        if (sensor.getMinThreshold() != null && request.getValue() < sensor.getMinThreshold()) {
            isAnomaly = true;
            log.warn("Anomaly detected - Value below minimum - SensorID: {}", sensorId);
        } else if (sensor.getMaxThreshold() != null && request.getValue() > sensor.getMaxThreshold()) {
            isAnomaly = true;
            log.warn("Anomaly detected - Value above maximum - SensorID: {}", sensorId);
        }

        SensorData sensorData = SensorData.builder()
            .sensorId(sensorId)
            .value(request.getValue())
            .unit(sensor.getUnit())
            .isAnomaly(isAnomaly)
            .timestamp(LocalDateTime.now())
            .createdBy("system")
            .build();

        return sensorDataRepository.save(sensorData);
    }

    /**
     * Record batch of sensor readings
     * @param sensorId Sensor ID
     * @param request Multiple readings
     */
    public void recordBatchSensorReadings(Long sensorId, SensorDataRequest request) {
        log.info("Recording batch sensor readings - SensorID: {}", sensorId);

        for (int i = 0; i < (request.getBatchSize() != null ? request.getBatchSize() : 1); i++) {
            recordSensorReading(sensorId, request);
        }

        log.debug("Batch recording completed - SensorID: {}", sensorId);
    }

    /**
     * Delete old sensor data (for database cleanup)
     * @param daysOld Number of days to keep
     */
    public void deleteOldSensorData(int daysOld) {
        log.info("Deleting sensor data older than {} days", daysOld);

        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysOld);
        sensorDataRepository.deleteOlderThan(cutoffDate);

        log.info("Sensor data cleanup completed - Cutoff date: {}", cutoffDate);
    }

    /**
     * Delete all data for a sensor
     * @param sensorId Sensor ID
     */
    public void deleteSensorData(Long sensorId) {
        log.info("Deleting all data for sensor: {}", sensorId);

        Sensor sensor = sensorRepository.findById(sensorId)
            .orElseThrow(() -> new EntityNotFoundException("Sensor not found: " + sensorId));

        // Delete all sensor data
        sensorDataRepository.findBySensorIdOrderByTimestampDesc(sensorId, org.springframework.data.domain.Pageable.unpaged())
            .forEach(sensorDataRepository::delete);

        log.debug("All data deleted for sensor: {}", sensorId);
    }
}
