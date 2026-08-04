package com.pfe.predictive.data.repository.finance;

import com.pfe.predictive.core.entity.finance.ExpenseCategory;
import com.pfe.predictive.core.entity.finance.ExpenseReport;
import com.pfe.predictive.core.entity.finance.ExpenseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    // Paginated variants - full expense history grows without bound over time.
    Page<ExpenseReport> findBySubmittedBy(String username, Pageable pageable);

    List<ExpenseReport> findByStatusOrderByCreatedDateDesc(ExpenseStatus status);

    Page<ExpenseReport> findByStatusOrderByCreatedDateDesc(ExpenseStatus status, Pageable pageable);

    List<ExpenseReport> findAllByOrderByCreatedDateDesc();

    Page<ExpenseReport> findAllByOrderByCreatedDateDesc(Pageable pageable);

    List<ExpenseReport> findByCategory(ExpenseCategory category);

    Page<ExpenseReport> findByCategory(ExpenseCategory category, Pageable pageable);

    List<ExpenseReport> findByMachineId(Long machineId);

    Page<ExpenseReport> findByMachineId(Long machineId, Pageable pageable);

    long countByStatus(ExpenseStatus status);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM ExpenseReport e " +
           "WHERE e.status = 'APPROVED' AND YEAR(e.createdDate) = :year")
    BigDecimal sumApprovedAmountByYear(@Param("year") int year);

    @Query("SELECT e FROM ExpenseReport e WHERE " +
           "YEAR(e.createdDate) = :year AND MONTH(e.createdDate) = :month")
    List<ExpenseReport> findByYearAndMonth(@Param("year") int year, @Param("month") int month);
}
