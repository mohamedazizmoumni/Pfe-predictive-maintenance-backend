package com.pfe.predictive.inventory.repository;

import com.pfe.predictive.inventory.entity.Part;
import com.pfe.predictive.inventory.entity.PartStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PartRepository extends JpaRepository<Part, Long> {

    List<Part> findByCategory(String category);

    List<Part> findBySubCategory(String subCategory);

    List<Part> findBySupplier(String supplier);

    List<Part> findByStatus(PartStatus status);

    long countByStatus(PartStatus status);

    @Query("SELECT p FROM Part p WHERE p.currentStock <= p.minimumStock ORDER BY p.currentStock ASC")
    List<Part> findLowStockParts();

    // Gives inventory.low-stock-threshold-percentage an actual meaning: an
    // early-warning buffer above the hard minimumStock line used by
    // findLowStockParts(), so a proactive reorder can be drafted before stock
    // is already critically low, not just once it is.
    @Query("SELECT p FROM Part p WHERE p.currentStock <= p.minimumStock * (1 + :thresholdPercentage / 100.0) ORDER BY p.currentStock ASC")
    List<Part> findPartsAtOrBelowReorderTrigger(double thresholdPercentage);

    @Query("SELECT p FROM Part p WHERE p.partNumber = :partNumber")
    Part findByPartNumber(String partNumber);

    @Query("SELECT p FROM Part p WHERE LOWER(p.name) = LOWER(:name)")
    java.util.Optional<Part> findByNameIgnoreCase(String name);

    boolean existsByPartNumber(String partNumber);

    boolean existsByPartNumberAndIdNot(String partNumber, Long id);

    @Query("SELECT DISTINCT p.category FROM Part p WHERE p.category IS NOT NULL ORDER BY p.category ASC")
    List<String> findDistinctCategories();
}
