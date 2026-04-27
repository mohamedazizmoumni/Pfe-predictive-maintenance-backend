package com.pfe.predictive.machine.service;

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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Machine Query Service
 * Handles read-only operations for machines, sensors, and sensor data
 * All operations are read-only for optimal database performance
 *
 * @author Machine Module
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class MachineQueryService {

    private final MachineRepository machineRepository;
    private final SensorRepository sensorRepository;
    private final SensorDataRepository sensorDataRepository;

    // ==================== MACHINE QUERIES ====================

    /**
     * Get machine by ID
     * @param machineId Machine ID
     * @return Machine or throw EntityNotFoundException
     */
    public Machine getMachineById(Long machineId) {
        log.debug("Fetching machine: {}", machineId);
        return machineRepository.findById(machineId)
            .orElseThrow(() -> new EntityNotFoundException("Machine not found: " + machineId));
    }

    /**
     * Get machine by serial number
     * @param serialNumber Serial number
     * @return Machine or throw EntityNotFoundException
     */
    public Machine getMachineBySerialNumber(String serialNumber) {
        log.debug("Fetching machine by serial: {}", serialNumber);
        return machineRepository.findBySerialNumber(serialNumber)
            .orElseThrow(() -> new EntityNotFoundException("Machine not found: " + serialNumber));
    }

    /**
     * Get all machines paginated
     * @param pageable Pagination info
     * @return Page of machines
     */
    public Page<Machine> getAllMachines(Pageable pageable) {
        log.debug("Fetching all machines, page: {}, size: {}", pageable.getPageNumber(), pageable.getPageSize());
        return machineRepository.findAll(pageable);
    }

    /**
     * Get machines by status
     * @param status Machine status
     * @param pageable Pagination info
     * @return Page of machines
     */
    public Page<Machine> getMachinesByStatus(MachineStatus status, Pageable pageable) {
        log.debug("Fetching machines by status: {}", status);
        return machineRepository.findByStatus(status, pageable);
    }

    /**
     * Get machines by location
     * @param location Machine location
     * @param pageable Pagination info
     * @return Page of machines
     */
    public Page<Machine> getMachinesByLocation(String location, Pageable pageable) {
        log.debug("Fetching machines by location: {}", location);
        return machineRepository.findByLocation(location, pageable);
    }

    /**
     * Get machines by model
     * @param model Machine model
     * @param pageable Pagination info
     * @return Page of machines
     */
    public Page<Machine> getMachinesByModel(String model, Pageable pageable) {
        log.debug("Fetching machines by model: {}", model);
        return machineRepository.findByModel(model, pageable);
    }

    /**
     * Get critical machines (faulty or in maintenance)
     * @param pageable Pagination info
     * @return Page of machines
     */
    public Page<Machine> getCriticalMachines(Pageable pageable) {
        log.debug("Fetching critical machines");
        return machineRepository.findCriticalMachines(pageable);
    }

    /**
     * Get operational machines
     * @param pageable Pagination info
     * @return Page of machines
     */
    public Page<Machine> getOperationalMachines(Pageable pageable) {
        return machineRepository.findByStatus(MachineStatus.OPERATIONAL, pageable);
    }

    /**
     * Get machines by status and location
     * @param status Machine status
     * @param location Location
     * @param pageable Pagination info
     * @return Page of machines
     */
    public Page<Machine> getMachinesByStatusAndLocation(MachineStatus status, String location, Pageable pageable) {
        log.debug("Fetching machines - Status: {}, Location: {}", status, location);
        return machineRepository.findByStatusAndLocation(status, location, pageable);
    }

    /**
     * Get machine statistics
     * @return Map of status to count
     */
    public Map<String, Long> getMachineStats() {
        log.debug("Calculating machine statistics");

        return MachineStatus.class.getEnumConstants() != null
            ? java.util.Arrays.stream(MachineStatus.class.getEnumConstants())
                .collect(Collectors.toMap(
                    Enum::toString,
                    status -> machineRepository.countByStatus((MachineStatus) status)
                ))
            : Map.of();
    }

    /**
     * Get total machine count
     * @return Total count
     */
    public long getTotalMachineCount() {
        return machineRepository.count();
    }

    // ==================== SENSOR QUERIES ====================

    /**
     * Get sensor by ID
     * @param sensorId Sensor ID
     * @return Sensor or throw EntityNotFoundException
     */
    public Sensor getSensorById(Long sensorId) {
        log.debug("Fetching sensor: {}", sensorId);
        return sensorRepository.findById(sensorId)
            .orElseThrow(() -> new EntityNotFoundException("Sensor not found: " + sensorId));
    }

    /**
     * Get all sensors for machine
     * @param machineId Machine ID
     * @return List of sensors
     */
    public List<Sensor> getMachineSensors(Long machineId) {
        log.debug("Fetching sensors for machine: {}", machineId);
        return sensorRepository.findByMachineId(machineId);
    }

    /**
     * Get sensor count for machine
     * @param machineId Machine ID
     * @return Sensor count
     */
    public long getSensorCountForMachine(Long machineId) {
        return sensorRepository.countByMachineId(machineId);
    }

    /**
     * Get sensors by type
     * @param sensorType Sensor type
     * @return List of sensors
     */
    public List<Sensor> getSensorsByType(String sensorType) {
        log.debug("Fetching sensors by type: {}", sensorType);
        return sensorRepository.findBySensorType(sensorType);
    }

    /**
     * Get sensor for machine by type
     * @param machineId Machine ID
     * @param sensorType Sensor type
     * @return Sensor or throw EntityNotFoundException
     */
    public Sensor getMachineSensorByType(Long machineId, String sensorType) {
        return sensorRepository.findByMachineIdAndSensorType(machineId, sensorType)
            .orElseThrow(() -> new EntityNotFoundException("Sensor not found for machine"));
    }

    // ==================== SENSOR DATA QUERIES ====================

    /**
     * Get latest sensor reading
     * @param sensorId Sensor ID
     * @return Latest SensorData
     */
    public SensorData getLatestSensorReading(Long sensorId) {
        log.debug("Fetching latest sensor reading: {}", sensorId);
        return sensorDataRepository.findFirstBySensorIdOrderByTimestampDesc(sensorId)
            .orElseThrow(() -> new EntityNotFoundException("No sensor data found"));
    }

    /**
     * Get recent sensor readings
     * @param sensorId Sensor ID
     * @param pageable Pagination info
     * @return Page of sensor data
     */
    public Page<SensorData> getRecentSensorReadings(Long sensorId, Pageable pageable) {
        log.debug("Fetching recent readings for sensor: {}", sensorId);
        return sensorDataRepository.findBySensorIdOrderByTimestampDesc(sensorId, pageable);
    }

    /**
     * Get sensor data within time range
     * @param sensorId Sensor ID
     * @param startTime Start time
     * @param endTime End time
     * @return List of sensor data
     */
    public List<SensorData> getSensorDataByTimeRange(Long sensorId, LocalDateTime startTime, LocalDateTime endTime) {
        log.debug("Fetching sensor data for range - SensorID: {}, Start: {}, End: {}", sensorId, startTime, endTime);
        return sensorDataRepository.findBySensorIdAndTimestampBetweenOrderByTimestampDesc(sensorId, startTime, endTime);
    }

    /**
     * Get sensor anomalies
     * @param sensorId Sensor ID
     * @param pageable Pagination info
     * @return Page of anomalies
     */
    public Page<SensorData> getSensorAnomalies(Long sensorId, Pageable pageable) {
        log.debug("Fetching anomalies for sensor: {}", sensorId);
        return sensorDataRepository.findBySensorIdAndIsAnomalyTrueOrderByTimestampDesc(sensorId, pageable);
    }

    /**
     * Get recent anomalies for sensor
     * @param sensorId Sensor ID
     * @param daysBack Number of days back
     * @return List of anomalies
     */
    public List<SensorData> getRecentAnomalies(Long sensorId, int daysBack) {
        LocalDateTime since = LocalDateTime.now().minusDays(daysBack);
        return sensorDataRepository.findRecentAnomalies(sensorId, since);
    }

    /**
     * Get anomaly count for sensor
     * @param sensorId Sensor ID
     * @return Count of anomalies
     */
    public long getAnomalyCount(Long sensorId) {
        return sensorDataRepository.countAnomaliesBySensor(sensorId);
    }

    /**
     * Get average value for sensor
     * @param sensorId Sensor ID
     * @return Average value or 0.0
     */
    public Double getAverageValue(Long sensorId) {
        Double avg = sensorDataRepository.getAverageValueBySensor(sensorId);
        return avg != null ? avg : 0.0;
    }

    /**
     * Get min-max range for sensor in time period
     * @param sensorId Sensor ID
     * @param startTime Start time
     * @param endTime End time
     * @return Map with min and max values
     */
    public Map<String, Double> getValueRange(Long sensorId, LocalDateTime startTime, LocalDateTime endTime) {
        Double maxValue = sensorDataRepository.getMaxValueInRange(sensorId, startTime, endTime);
        Double minValue = sensorDataRepository.getMinValueInRange(sensorId, startTime, endTime);

        return Map.of(
            "max", maxValue != null ? maxValue : 0.0,
            "min", minValue != null ? minValue : 0.0
        );
    }

    /**
     * Get sensor data count
     * @param sensorId Sensor ID
     * @return Total count
     */
    public long getSensorDataCount(Long sensorId) {
        return sensorDataRepository.countBySensorId(sensorId);
    }

    /**
     * Get sensor health status (based on anomaly ratio)
     * @param sensorId Sensor ID
     * @return Health percentage (0-100)
     */
    public int getSensorHealth(Long sensorId) {
        long total = sensorDataRepository.countBySensorId(sensorId);
        if (total == 0) return 100;

        long anomalies = sensorDataRepository.countAnomaliesBySensor(sensorId);
        return (int) ((total - anomalies) * 100 / total);
    }
}
