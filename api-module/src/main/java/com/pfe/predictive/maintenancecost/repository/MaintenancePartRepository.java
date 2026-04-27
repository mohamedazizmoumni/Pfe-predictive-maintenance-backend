package com.pfe.predictive.maintenancecost.repository;

import com.pfe.predictive.maintenancecost.entity.MaintenancePart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MaintenancePartRepository extends JpaRepository<MaintenancePart, Long> {

    Optional<MaintenancePart> findByReferenceCode(String referenceCode);
}
