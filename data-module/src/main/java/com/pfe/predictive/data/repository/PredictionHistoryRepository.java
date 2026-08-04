package com.pfe.predictive.data.repository;

import com.pfe.predictive.core.entity.PredictionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Prediction History Repository
 * 
 * Provides data access for prediction history records.
 */
@Repository
public interface PredictionHistoryRepository extends JpaRepository<PredictionHistory, Long> {
    
    /**
     * Find prediction history for a specific machine.
     */
    List<PredictionHistory> findByMachineIdOrderByTimestampDesc(Long machineId);
    
    /**
     * Find recent predictions for a machine.
     */
    List<PredictionHistory> findTop10ByMachineIdOrderByTimestampDesc(Long machineId);

    /**
     * Most recent prediction for a machine — feeds the explainability endpoint.
     */
    java.util.Optional<PredictionHistory> findFirstByMachineIdOrderByTimestampDesc(Long machineId);
    
    /**
     * Find predictions within a time range.
     */
    List<PredictionHistory> findByMachineIdAndTimestampBetweenOrderByTimestampDesc(
        Long machineId,
        LocalDateTime startTime,
        LocalDateTime endTime
    );
    
    /**
     * Find critical predictions.
     */
    List<PredictionHistory> findByRiskLevelOrderByTimestampDesc(String riskLevel);
    
    /**
     * Find predictions requiring immediate action.
     */
    List<PredictionHistory> findByRequiresImmediateActionTrueOrderByTimestampDesc();
    
    /**
     * Get average anomaly probability for a machine over time.
     */
    @Query("SELECT AVG(p.anomalyProbability) FROM PredictionHistory p " +
           "WHERE p.machineId = :machineId " +
           "AND p.timestamp >= :since")
    Double getAverageAnomalyProbability(
        @Param("machineId") Long machineId,
        @Param("since") LocalDateTime since
    );
    
    /**
     * Count critical predictions for a machine.
     */
    @Query("SELECT COUNT(p) FROM PredictionHistory p " +
           "WHERE p.machineId = :machineId " +
           "AND p.riskLevel = 'CRITICAL' " +
           "AND p.timestamp >= :since")
    Long countCriticalPredictions(
        @Param("machineId") Long machineId,
        @Param("since") LocalDateTime since
    );
}
