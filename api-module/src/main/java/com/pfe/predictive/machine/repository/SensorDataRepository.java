package com.pfe.predictive.machine.repository;

import com.pfe.predictive.core.entity.SensorData;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository("machineSensorDataRepository")
public interface SensorDataRepository extends JpaRepository<SensorData, Long> {

    Optional<SensorData> findFirstBySensorIdOrderByRecordedDateDesc(Long sensorId);

    Page<SensorData> findBySensorIdOrderByRecordedDateDesc(Long sensorId, Pageable pageable);

    Page<SensorData> findBySensorMachineIdOrderByRecordedDateDesc(Long machineId, Pageable pageable);

    Page<SensorData> findAllByOrderByRecordedDateDesc(Pageable pageable);

    long countBySensorMachineIdAndIsAnomalyTrue(Long machineId);
}
