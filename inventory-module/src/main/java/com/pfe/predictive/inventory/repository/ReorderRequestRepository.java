package com.pfe.predictive.inventory.repository;

import com.pfe.predictive.inventory.entity.ReorderRequest;
import com.pfe.predictive.inventory.entity.ReorderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReorderRequestRepository extends JpaRepository<ReorderRequest, Long> {

    List<ReorderRequest> findByStatus(ReorderStatus status);

    List<ReorderRequest> findByStatusOrderByCreatedDateDesc(ReorderStatus status);

    long countByStatus(ReorderStatus status);

    List<ReorderRequest> findByPartId(Long partId);

    boolean existsByPartIdAndStatus(Long partId, ReorderStatus status);

    @Query("SELECT r FROM ReorderRequest r WHERE r.status = 'REQUESTED' ORDER BY r.createdDate ASC")
    List<ReorderRequest> findPendingReorders();
}
