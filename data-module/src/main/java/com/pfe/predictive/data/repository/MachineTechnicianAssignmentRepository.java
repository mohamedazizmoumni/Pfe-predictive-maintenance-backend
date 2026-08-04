package com.pfe.predictive.data.repository;

import com.pfe.predictive.core.entity.MachineTechnicianAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MachineTechnicianAssignmentRepository extends JpaRepository<MachineTechnicianAssignment, Long> {
    List<MachineTechnicianAssignment> findByMachineId(Long machineId);
    List<MachineTechnicianAssignment> findByTechnicianId(Long technicianId);
    Optional<MachineTechnicianAssignment> findByMachineIdAndTechnicianId(Long machineId, Long technicianId);
    boolean existsByMachineIdAndTechnicianId(Long machineId, Long technicianId);
    void deleteByMachineIdAndTechnicianId(Long machineId, Long technicianId);

    @Query("SELECT a.machine.id FROM MachineTechnicianAssignment a WHERE a.technician.id = :technicianId")
    List<Long> findMachineIdsByTechnicianId(@Param("technicianId") Long technicianId);
}
