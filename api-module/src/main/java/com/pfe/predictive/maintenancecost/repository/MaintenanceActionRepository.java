package com.pfe.predictive.maintenancecost.repository;

import com.pfe.predictive.maintenancecost.entity.MaintenanceAction;
import com.pfe.predictive.maintenancecost.entity.Machine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MaintenanceActionRepository extends JpaRepository<MaintenanceAction, Long> {

    List<MaintenanceAction> findByMachineOrderByScheduledDateDesc(Machine machine);
}
