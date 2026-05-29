package com.pfe.predictive.data.repository;

import com.pfe.predictive.core.entity.MachineSimulationState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for MachineSimulationState - Physics-based degradation state.
 * 
 * This is the SINGLE SOURCE OF TRUTH for machine condition.
 */
@Repository
public interface MachineSimulationStateRepository extends JpaRepository<MachineSimulationState, Long> {
    
    /**
     * Find simulation state by machine ID.
     */
    Optional<MachineSimulationState> findByMachineId(Long machineId);
    
    /**
     * Find all machines with health below threshold.
     */
    @Query("SELECT s FROM MachineSimulationState s WHERE s.health < :threshold ORDER BY s.health ASC")
    List<MachineSimulationState> findUnhealthyMachines(@Param("threshold") double threshold);
    
    /**
     * Find all machines with high bearing wear.
     */
    @Query("SELECT s FROM MachineSimulationState s WHERE s.bearingWear > :threshold ORDER BY s.bearingWear DESC")
    List<MachineSimulationState> findMachinesWithHighBearingWear(@Param("threshold") double threshold);
    
    /**
     * Find all machines with low lubrication.
     */
    @Query("SELECT s FROM MachineSimulationState s WHERE s.lubricationLevel < :threshold ORDER BY s.lubricationLevel ASC")
    List<MachineSimulationState> findMachinesWithLowLubrication(@Param("threshold") double threshold);
    
    /**
     * Find all machines with high thermal stress.
     */
    @Query("SELECT s FROM MachineSimulationState s WHERE s.thermalStress > :threshold ORDER BY s.thermalStress DESC")
    List<MachineSimulationState> findMachinesWithHighThermalStress(@Param("threshold") double threshold);
    
    /**
     * Find all machines with high fatigue.
     */
    @Query("SELECT s FROM MachineSimulationState s WHERE s.fatigueIndex > :threshold ORDER BY s.fatigueIndex DESC")
    List<MachineSimulationState> findMachinesWithHighFatigue(@Param("threshold") double threshold);
    
    /**
     * Find all critical machines (health < 30 OR bearingWear > 80 OR lubricationLevel < 20).
     */
    @Query("SELECT s FROM MachineSimulationState s WHERE s.health < 30 OR s.bearingWear > 80 OR s.lubricationLevel < 20 OR s.fatigueIndex > 85")
    List<MachineSimulationState> findCriticalMachines();
    
    /**
     * Count machines by health range.
     */
    @Query("SELECT COUNT(s) FROM MachineSimulationState s WHERE s.health >= :minHealth AND s.health < :maxHealth")
    long countByHealthRange(@Param("minHealth") double minHealth, @Param("maxHealth") double maxHealth);
    
    /**
     * Get average health across all machines.
     */
    @Query("SELECT AVG(s.health) FROM MachineSimulationState s")
    Double getAverageHealth();
    
    /**
     * Get machines needing maintenance (based on cycles or degradation).
     */
    @Query("SELECT s FROM MachineSimulationState s WHERE s.cyclesSinceLastMaintenance > :cycleThreshold OR s.health < :healthThreshold")
    List<MachineSimulationState> findMachinesNeedingMaintenance(
        @Param("cycleThreshold") int cycleThreshold,
        @Param("healthThreshold") double healthThreshold
    );
}
