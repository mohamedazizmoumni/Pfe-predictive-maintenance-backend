package com.pfe.predictive.data.repository.portal;

import com.pfe.predictive.core.entity.portal.Warranty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WarrantyRepository extends JpaRepository<Warranty, Long> {

    List<Warranty> findByMachineIdOrderByEndDateDesc(Long machineId);

    List<Warranty> findByMachineIdInOrderByEndDateDesc(List<Long> machineIds);
}
