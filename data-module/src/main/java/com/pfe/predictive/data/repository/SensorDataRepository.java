package com.pfe.predictive.data.repository;

import com.pfe.predictive.core.entity.SensorData;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SensorDataRepository extends JpaRepository<SensorData, Long> {

    Optional<SensorData> findFirstBySensorIdOrderByRecordedDateDesc(Long sensorId);

    Page<SensorData> findBySensorIdOrderByRecordedDateDesc(Long sensorId, Pageable pageable);

    @Query("SELECT s FROM SensorData s WHERE s.sensor.id IN (SELECT sen.id FROM Sensor sen WHERE sen.machine.id = :machineId) ORDER BY s.recordedDate DESC")
    Page<SensorData> findBySensorMachineIdOrderByRecordedDateDesc(Long machineId, Pageable pageable);

    Page<SensorData> findAllByOrderByRecordedDateDesc(Pageable pageable);
}
