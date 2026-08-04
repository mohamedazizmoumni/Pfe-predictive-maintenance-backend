package com.pfe.predictive.data.repository;

import com.pfe.predictive.core.entity.EnvironmentalTelemetry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EnvironmentalTelemetryRepository extends JpaRepository<EnvironmentalTelemetry, Long> {

    Optional<EnvironmentalTelemetry> findTop1ByMachine_IdOrderByTimestampDesc(Long machineId);
}
