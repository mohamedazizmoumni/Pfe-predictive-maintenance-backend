package com.pfe.predictive.machine.repository;

import com.pfe.predictive.core.entity.Machine;
import com.pfe.predictive.core.entity.MachineStatus;
import com.pfe.predictive.core.entity.Sensor;
import com.pfe.predictive.core.entity.SensorData;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Machine, Sensor, and SensorData repositories
 * Handles all database operations for machine monitoring data
 *
 * @author Machine Module
 * @version 1.0
 */

@Repository
public interface MachineRepository extends JpaRepository<Machine, Long> {

    /**
     * Find machine by serial number (unique)
     */
    Optional<Machine> findBySerialNumber(String serialNumber);

    /**
     * Find by machine model
     */
    Page<Machine> findByModel(String model, Pageable pageable);

    /**
     * Find by status
     */
    Page<Machine> findByStatus(MachineStatus status, Pageable pageable);

    /**
     * Find machines in specific location
     */
    Page<Machine> findByLocation(String location, Pageable pageable);

    /**
     * Find machines by status and location
     */
    Page<Machine> findByStatusAndLocation(MachineStatus status, String location, Pageable pageable);

    /**
     * Find critical/faulty machines
     */
    @Query("SELECT m FROM Machine m WHERE m.status IN ('FAULTY', 'MAINTENANCE')")
    Page<Machine> findCriticalMachines(Pageable pageable);

    /**
     * Count machines by status
     */
    long countByStatus(MachineStatus status);

    /**
     * Find machines installed between dates
     */
    List<Machine> findByInstalledDateBetweenOrderByInstalledDateDesc(LocalDateTime start, LocalDateTime end);

    /**
     * Check if serial number exists
     */
    boolean existsBySerialNumber(String serialNumber);
}

@Repository
public interface SensorRepository extends JpaRepository<Sensor, Long> {

    /**
     * Find all sensors for a machine
     */
    List<Sensor> findByMachineId(Long machineId);

    /**
     * Find sensor by machine and sensor type
     */
    Optional<Sensor> findByMachineIdAndSensorType(Long machineId, String sensorType);

    /**
     * Find all sensors of specific type
     */
    List<Sensor> findBySensorType(String sensorType);

    /**
     * Count sensors per machine
     */
    long countByMachineId(Long machineId);
}

@Repository
public interface SensorDataRepository extends JpaRepository<SensorData, Long> {

    /**
     * Find recent sensor data for a sensor, paginated
     */
    Page<SensorData> findBySensorIdOrderByTimestampDesc(Long sensorId, Pageable pageable);

    /**
     * Find sensor data within date range
     */
    List<SensorData> findBySensorIdAndTimestampBetweenOrderByTimestampDesc(
        Long sensorId,
        LocalDateTime startTime,
        LocalDateTime endTime
    );

    /**
     * Get latest sensor reading
     */
    Optional<SensorData> findFirstBySensorIdOrderByTimestampDesc(Long sensorId);

    /**
     * Find sensor data with anomalies (isAnomaly = true)
     */
    Page<SensorData> findBySensorIdAndIsAnomalyTrueOrderByTimestampDesc(Long sensorId, Pageable pageable);

    /**
     * Count data points per sensor
     */
    long countBySensorId(Long sensorId);

    /**
     * Find average value for sensor data
     */
    @Query("SELECT AVG(sd.value) FROM SensorData sd WHERE sd.sensorId = :sensorId")
    Double getAverageValueBySensor(@Param("sensorId") Long sensorId);

    /**
     * Find max value in time range
     */
    @Query("SELECT MAX(sd.value) FROM SensorData sd WHERE sd.sensorId = :sensorId AND sd.timestamp BETWEEN :start AND :end")
    Double getMaxValueInRange(@Param("sensorId") Long sensorId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Find min value in time range
     */
    @Query("SELECT MIN(sd.value) FROM SensorData sd WHERE sd.sensorId = :sensorId AND sd.timestamp BETWEEN :start AND :end")
    Double getMinValueInRange(@Param("sensorId") Long sensorId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Find recent anomalies
     */
    @Query("SELECT sd FROM SensorData sd WHERE sd.sensorId = :sensorId AND sd.isAnomaly = true AND sd.timestamp > :since ORDER BY sd.timestamp DESC")
    List<SensorData> findRecentAnomalies(@Param("sensorId") Long sensorId, @Param("since") LocalDateTime since);

    /**
     * Get anomaly count for sensor
     */
    @Query("SELECT COUNT(sd) FROM SensorData sd WHERE sd.sensorId = :sensorId AND sd.isAnomaly = true")
    long countAnomaliesBySensor(@Param("sensorId") Long sensorId);

    /**
     * Delete old data (for cleanup jobs)
     */
    @Query("DELETE FROM SensorData sd WHERE sd.timestamp < :cutoffDate")
    void deleteOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);
}
