package com.pfe.predictive.inventory.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Holds stock against a specific job before it's consumed. currentStock on
 * Part is NOT decremented at reservation time — only on consume(); available
 * stock is computed as currentStock minus the sum of active RESERVED rows,
 * to avoid a running counter that can drift out of sync.
 */
@Entity
@Table(name = "part_reservations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long partId;

    @Column(nullable = false)
    private Integer quantityReserved;

    private Long maintenanceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PartReservationStatus status;

    @Column(nullable = false, length = 100)
    private String reservedBy;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime reservedAt;

    private LocalDateTime resolvedAt;
}
