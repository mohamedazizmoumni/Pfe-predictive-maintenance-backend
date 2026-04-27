package com.pfe.predictive.data.repository;

import com.pfe.predictive.core.entity.Machine;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MachineRepository extends JpaRepository<Machine, Long> {
    Optional<Machine> findBySerialNumber(String serialNumber);
    Page<Machine> findByStatus(String status, Pageable pageable);
    Page<Machine> findByLocation(String location, Pageable pageable);
    boolean existsBySerialNumber(String serialNumber);
}
