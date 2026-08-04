package com.pfe.predictive.inventory.repository;

import com.pfe.predictive.inventory.entity.InventoryUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface InventoryUsageRepository extends JpaRepository<InventoryUsage, Long> {

    List<InventoryUsage> findByPartId(Long partId);

    List<InventoryUsage> findByTaskId(Long taskId);

    List<InventoryUsage> findByUsedBy(String usedBy);

    @Query("SELECT SUM(u.quantityUsed) FROM InventoryUsage u WHERE u.part.id = :partId")
    Integer getTotalUsageForPart(Long partId);

    @Query("SELECT u FROM InventoryUsage u WHERE u.part.id = :partId AND u.usedDate >= :since ORDER BY u.usedDate DESC")
    List<InventoryUsage> findByPartIdSince(@Param("partId") Long partId, @Param("since") LocalDateTime since);

    @Query("SELECT u FROM InventoryUsage u WHERE u.usedDate >= :since ORDER BY u.usedDate DESC")
    List<InventoryUsage> findAllSince(@Param("since") LocalDateTime since);

    List<InventoryUsage> findAllByOrderByUsedDateDesc();
}
