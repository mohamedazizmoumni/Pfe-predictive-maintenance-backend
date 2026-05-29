package com.pfe.predictive.data.repository.finance;

import com.pfe.predictive.core.entity.finance.ExpenseCategory;
import com.pfe.predictive.core.entity.finance.ExpenseReport;
import com.pfe.predictive.core.entity.finance.ExpenseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ExpenseReportRepository extends JpaRepository<ExpenseReport, Long> {

    List<ExpenseReport> findByStatus(ExpenseStatus status);

    List<ExpenseReport> findBySubmittedBy(String username);

    List<ExpenseReport> findByStatusOrderByCreatedDateDesc(ExpenseStatus status);

    List<ExpenseReport> findAllByOrderByCreatedDateDesc();

    List<ExpenseReport> findByCategory(ExpenseCategory category);

    List<ExpenseReport> findByMachineId(Long machineId);

    long countByStatus(ExpenseStatus status);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM ExpenseReport e " +
           "WHERE e.status = 'APPROVED' AND YEAR(e.createdDate) = :year")
    BigDecimal sumApprovedAmountByYear(@Param("year") int year);

    @Query("SELECT e FROM ExpenseReport e WHERE " +
           "YEAR(e.createdDate) = :year AND MONTH(e.createdDate) = :month")
    List<ExpenseReport> findByYearAndMonth(@Param("year") int year, @Param("month") int month);
}
