package com.pfe.predictive.data.repository;

import com.pfe.predictive.core.entity.PredictionRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for PredictionRecord entity.
 * Handles persistence and querying of ML prediction results with audit trails.
 */
@Repository
public interface PredictionRecordRepository extends JpaRepository<PredictionRecord, Long> {

    /**
     * Find all predictions for a machine, most recent first.
     * @param machineId ID of the machine
     * @param pageable pagination parameters
     * @return page of prediction records
     */
    Page<PredictionRecord> findByMachineIdOrderByPredictedAtDesc(Long machineId, Pageable pageable);

    /**
     * Find predictions for a machine within a time range.
     * Used by the /trend endpoint, which aggregates the whole range into
     * daily averages and so genuinely needs every row - do not paginate this
     * overload. For a paginated view of the same range, use the Pageable
     * overload below instead.
     * @param machineId ID of the machine
     * @param fromDate start of time range (inclusive)
     * @param toDate end of time range (inclusive)
     * @return list of prediction records
     */
    List<PredictionRecord> findByMachineIdAndPredictedAtBetween(
        Long machineId,
        LocalDateTime fromDate,
        LocalDateTime toDate
    );

    /**
     * Paginated variant of the above - for endpoints that show a page of
     * records in a date range rather than aggregating the whole range.
     */
    Page<PredictionRecord> findByMachineIdAndPredictedAtBetween(
        Long machineId,
        LocalDateTime fromDate,
        LocalDateTime toDate,
        Pageable pageable
    );

    /**
     * Calculate average RUL for a machine.
     * Used for fleet health dashboards and analytics.
     * @param machineId ID of the machine
     * @return average RUL value, or null if no predictions exist
     */
    @Query("SELECT AVG(p.rulValue) FROM PredictionRecord p WHERE p.machineId = :machineId")
    Double findAverageRulByMachineId(@Param("machineId") Long machineId);

    /**
     * Find the most recent prediction for a machine.
     * @param machineId ID of the machine
     * @param pageable must have size=1 to get single result
     * @return page with at most one prediction record
     */
    @Query("SELECT p FROM PredictionRecord p WHERE p.machineId = :machineId ORDER BY p.predictedAt DESC")
    Page<PredictionRecord> findMostRecentByMachineId(@Param("machineId") Long machineId, Pageable pageable);

    /**
     * Count predictions for a machine with critical risk level.
     * @param machineId ID of the machine
     * @return count of critical predictions
     */
    @Query("SELECT COUNT(p) FROM PredictionRecord p WHERE p.machineId = :machineId AND p.riskLevel = 'CRITICAL'")
    long countCriticalPredictionsByMachineId(@Param("machineId") Long machineId);

    /**
     * Find all predictions across fleet with CRITICAL risk level.
     * Used for dashboard alerts and KPIs.
     * @return list of critical predictions
     */
    @Query("SELECT p FROM PredictionRecord p WHERE p.riskLevel = 'CRITICAL' ORDER BY p.predictedAt DESC")
    List<PredictionRecord> findAllCriticalPredictions();

    /**
     * Paginated variant of the above - fetches only the requested page from
     * the database instead of loading every critical prediction into memory.
     */
    @Query("SELECT p FROM PredictionRecord p WHERE p.riskLevel = 'CRITICAL' ORDER BY p.predictedAt DESC")
    Page<PredictionRecord> findAllCriticalPredictions(Pageable pageable);

    /**
     * Calculate average RUL across entire fleet.
     * @return fleet-wide average RUL
     */
    @Query("SELECT AVG(p.rulValue) FROM PredictionRecord p")
    Double findFleetAverageRul();
}
