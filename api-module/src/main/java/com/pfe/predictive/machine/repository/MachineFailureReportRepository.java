package com.pfe.predictive.machine.repository;

import com.pfe.predictive.machine.entity.MachineFailureReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface MachineFailureReportRepository extends JpaRepository<MachineFailureReport, Long> {

    boolean existsByMachineId(Long machineId);

    boolean existsByMachineIdAndCreatedAtAfter(Long machineId, LocalDateTime createdAtAfter);

    Page<MachineFailureReport> findByMachineIdOrderByCreatedAtDesc(Long machineId, Pageable pageable);

    Page<MachineFailureReport> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
