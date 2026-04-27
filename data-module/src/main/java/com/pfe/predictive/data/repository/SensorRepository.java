package com.pfe.predictive.data.repository;

import com.pfe.predictive.core.entity.Sensor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SensorRepository extends JpaRepository<Sensor, Long> {
    Optional<Sensor> findByCode(String code);
    List<Sensor> findByMachineId(Long machineId);
    boolean existsByCode(String code);
}
