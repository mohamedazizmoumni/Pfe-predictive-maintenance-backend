package com.pfe.predictive.data.repository.finance;

import com.pfe.predictive.core.entity.finance.AnnualBudget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AnnualBudgetRepository extends JpaRepository<AnnualBudget, Long> {

    Optional<AnnualBudget> findByYear(Integer year);
}
