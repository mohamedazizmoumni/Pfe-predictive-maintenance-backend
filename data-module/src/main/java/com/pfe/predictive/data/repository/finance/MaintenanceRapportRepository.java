package com.pfe.predictive.data.repository.finance;

import com.pfe.predictive.core.entity.finance.MaintenanceRapport;
import com.pfe.predictive.core.entity.finance.RapportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MaintenanceRapportRepository extends JpaRepository<MaintenanceRapport, Long> {

    List<MaintenanceRapport> findByTechnicianUsername(String username);

    // Paginated variant - rapport history grows without bound over time.
    Page<MaintenanceRapport> findByTechnicianUsername(String username, Pageable pageable);

    List<MaintenanceRapport> findAllByOrderByCreatedDateDesc();

    Page<MaintenanceRapport> findAllByOrderByCreatedDateDesc(Pageable pageable);

    List<MaintenanceRapport> findByStatusOrderByCreatedDateDesc(RapportStatus status);

    Page<MaintenanceRapport> findByStatusOrderByCreatedDateDesc(RapportStatus status, Pageable pageable);

    List<MaintenanceRapport> findByMachineId(Long machineId);

    long countByStatus(RapportStatus status);
}
