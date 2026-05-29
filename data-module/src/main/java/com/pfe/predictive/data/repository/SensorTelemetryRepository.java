package com.pfe.predictive.data.repository;

import com.pfe.predictive.core.entity.SensorTelemetry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for SensorTelemetry - Time-series sensor data storage.
 * 
 * Stores ALL sensor readings (no overwrite) for historical analysis.
 */
@Repository
public interface SensorTelemetryRepository extends JpaRepository<SensorTelemetry, Long> {
    
    /**
     * Find all telemetry for a machine within a time range.
     * Used for 24h rolling window queries.
     */
    List<SensorTelemetry> findByMachineIdAndTimestampBetween(
        Long machineId, 
        LocalDateTime start, 
        LocalDateTime end
    );
    
    /**
     * Find all telemetry for a machine after a specific time.
     */
    List<SensorTelemetry> findByMachineIdAndTimestampAfter(
        Long machineId, 
        LocalDateTime since
    );
    
    /**
     * Find latest telemetry reading for a machine.
     */
    @Query("SELECT t FROM SensorTelemetry t WHERE t.machineId = :machineId ORDER BY t.timestamp DESC LIMIT 1")
    Optional<SensorTelemetry> findLatestByMachineId(@Param("machineId") Long machineId);
    
    /**
     * Find telemetry for a machine, ordered by timestamp descending.
     */
    List<SensorTelemetry> findByMachineIdOrderByTimestampDesc(Long machineId);
    
    /**
     * Count telemetry records for a machine.
     */
    long countByMachineId(Long machineId);
    
    /**
     * Count telemetry records for a machine within a time range.
     */
    long countByMachineIdAndTimestampBetween(
        Long machineId,
        LocalDateTime start,
        LocalDateTime end
    );
    
    /**
     * Delete old telemetry data (retention policy).
     * Use with @Modifying and @Transactional.
     */
    @Modifying
    @Query("DELETE FROM SensorTelemetry t WHERE t.timestamp < :cutoffDate")
    int deleteByTimestampBefore(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    /**
     * Get average sensor values for a machine within a time range.
     */
    @Query("SELECT AVG(t.sensor1) FROM SensorTelemetry t WHERE t.machineId = :machineId AND t.timestamp BETWEEN :start AND :end")
    Double getAverageTemperature(@Param("machineId") Long machineId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    @Query("SELECT AVG(t.sensor2) FROM SensorTelemetry t WHERE t.machineId = :machineId AND t.timestamp BETWEEN :start AND :end")
    Double getAverageVibration(@Param("machineId") Long machineId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    @Query("SELECT AVG(t.sensor3) FROM SensorTelemetry t WHERE t.machineId = :machineId AND t.timestamp BETWEEN :start AND :end")
    Double getAveragePower(@Param("machineId") Long machineId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * Get max sensor values for a machine within a time range.
     */
    @Query("SELECT MAX(t.sensor1) FROM SensorTelemetry t WHERE t.machineId = :machineId AND t.timestamp BETWEEN :start AND :end")
    Double getMaxTemperature(@Param("machineId") Long machineId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    @Query("SELECT MAX(t.sensor2) FROM SensorTelemetry t WHERE t.machineId = :machineId AND t.timestamp BETWEEN :start AND :end")
    Double getMaxVibration(@Param("machineId") Long machineId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * Get min sensor values for a machine within a time range.
     */
    @Query("SELECT MIN(t.sensor1) FROM SensorTelemetry t WHERE t.machineId = :machineId AND t.timestamp BETWEEN :start AND :end")
    Double getMinTemperature(@Param("machineId") Long machineId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
