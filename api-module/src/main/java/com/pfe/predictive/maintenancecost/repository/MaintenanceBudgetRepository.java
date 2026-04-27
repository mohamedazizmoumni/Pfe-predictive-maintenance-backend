package com.pfe.predictive.maintenancecost.repository;

import com.pfe.predictive.maintenancecost.entity.MaintenanceBudget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MaintenanceBudgetRepository extends JpaRepository<MaintenanceBudget, Long> {

    Optional<MaintenanceBudget> findByDepartmentAndPeriod(String department, String period);
}