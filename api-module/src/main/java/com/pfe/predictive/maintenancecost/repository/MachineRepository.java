package com.pfe.predictive.maintenancecost.repository;

import com.pfe.predictive.maintenancecost.entity.Machine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository("maintenanceCostMachineRepository")
public interface MachineRepository extends JpaRepository<Machine, Long> {
}
