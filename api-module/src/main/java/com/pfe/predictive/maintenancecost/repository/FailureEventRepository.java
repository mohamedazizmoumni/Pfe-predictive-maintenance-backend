package com.pfe.predictive.maintenancecost.repository;

import com.pfe.predictive.maintenancecost.entity.FailureEvent;
import com.pfe.predictive.maintenancecost.entity.Machine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FailureEventRepository extends JpaRepository<FailureEvent, Long> {

    List<FailureEvent> findByMachineOrderByOccurredAtDesc(Machine machine);
}
