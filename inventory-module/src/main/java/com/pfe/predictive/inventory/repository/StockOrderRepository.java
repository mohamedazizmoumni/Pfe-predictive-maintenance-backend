package com.pfe.predictive.inventory.repository;

import com.pfe.predictive.inventory.entity.StockOrder;
import com.pfe.predictive.inventory.entity.StockOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockOrderRepository extends JpaRepository<StockOrder, Long> {

    List<StockOrder> findByStatus(StockOrderStatus status);

    long countByStatus(StockOrderStatus status);

    List<StockOrder> findByPartId(Long partId);

    List<StockOrder> findByOrderedBy(String orderedBy);

    @Query("SELECT so FROM StockOrder so WHERE so.status IN ('PENDING', 'SHIPPED') ORDER BY so.orderedDate ASC")
    List<StockOrder> findOpenOrders();
}
