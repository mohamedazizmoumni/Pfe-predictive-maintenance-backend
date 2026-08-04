package com.pfe.predictive.inventory.repository;

import com.pfe.predictive.inventory.entity.PartReservation;
import com.pfe.predictive.inventory.entity.PartReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PartReservationRepository extends JpaRepository<PartReservation, Long> {

    List<PartReservation> findByPartIdAndStatus(Long partId, PartReservationStatus status);

    List<PartReservation> findByMaintenanceId(Long maintenanceId);

    @Query("SELECT COALESCE(SUM(r.quantityReserved), 0) FROM PartReservation r " +
           "WHERE r.partId = :partId AND r.status = 'RESERVED'")
    Integer sumReservedQuantity(@Param("partId") Long partId);

    @Query("SELECT r.partId AS partId, SUM(r.quantityReserved) AS total FROM PartReservation r " +
           "WHERE r.status = 'RESERVED' GROUP BY r.partId")
    List<PartIdAndReserved> sumReservedQuantityByPart();

    interface PartIdAndReserved {
        Long getPartId();
        Integer getTotal();
    }
}
