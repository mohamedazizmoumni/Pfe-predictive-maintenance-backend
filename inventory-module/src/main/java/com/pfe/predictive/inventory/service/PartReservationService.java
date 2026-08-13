package com.pfe.predictive.inventory.service;

import com.pfe.predictive.inventory.dto.PartReservationRequest;
import com.pfe.predictive.inventory.dto.PartReservationResponse;
import com.pfe.predictive.inventory.entity.Part;
import com.pfe.predictive.inventory.entity.PartReservation;
import com.pfe.predictive.inventory.entity.PartReservationStatus;
import com.pfe.predictive.inventory.repository.PartRepository;
import com.pfe.predictive.inventory.repository.PartReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PartReservationService {

    private final PartReservationRepository reservationRepository;
    private final PartRepository partRepository;

    public PartReservationResponse reserve(PartReservationRequest request, String reservedBy) {
        Part part = partRepository.findById(request.getPartId())
                .orElseThrow(() -> new IllegalArgumentException("Part not found: " + request.getPartId()));

        int available = availableStock(part);
        if (request.getQuantity() > available) {
            throw new IllegalStateException(
                    "Only " + available + " unit(s) of \"" + part.getName() + "\" are available to reserve");
        }

        PartReservation reservation = PartReservation.builder()
                .partId(part.getId())
                .quantityReserved(request.getQuantity())
                .maintenanceId(request.getMaintenanceId())
                .status(PartReservationStatus.RESERVED)
                .reservedBy(reservedBy)
                .build();

        return toResponse(reservationRepository.save(reservation), part.getName());
    }

    public PartReservationResponse release(Long id) {
        PartReservation reservation = getActiveOrThrow(id);
        reservation.setStatus(PartReservationStatus.RELEASED);
        reservation.setResolvedAt(LocalDateTime.now());
        return toResponse(reservationRepository.save(reservation), partName(reservation.getPartId()));
    }

    /** Converts a reservation into an actual stock decrement — the part is now used, not just held. */
    public PartReservationResponse consume(Long id) {
        return consume(id, null);
    }

    /**
     * Same as consume(id), but for an actual-used quantity that may be less
     * than what was reserved (the acceptance case: reserve 2, technician
     * only uses 1). quantityUsed is clamped to [0, quantityReserved] — it
     * can't consume more than this reservation holds; a caller needing more
     * than was reserved must cover the excess through another path (e.g. a
     * second reservation, or MaintenanceRapportService's name-match
     * fallback for unreserved stock). Null quantityUsed means "consume the
     * full reserved amount", matching the original single-arg behavior.
     */
    public PartReservationResponse consume(Long id, Integer quantityUsed) {
        PartReservation reservation = getActiveOrThrow(id);
        Part part = partRepository.findById(reservation.getPartId())
                .orElseThrow(() -> new IllegalArgumentException("Part not found: " + reservation.getPartId()));

        int actualConsumed = quantityUsed == null
                ? reservation.getQuantityReserved()
                : Math.max(0, Math.min(quantityUsed, reservation.getQuantityReserved()));

        part.setCurrentStock(Math.max(0, part.getCurrentStock() - actualConsumed));
        partRepository.save(part);

        reservation.setQuantityConsumed(actualConsumed);
        reservation.setStatus(PartReservationStatus.CONSUMED);
        reservation.setResolvedAt(LocalDateTime.now());
        return toResponse(reservationRepository.save(reservation), part.getName());
    }

    /**
     * Active (RESERVED) reservations for a given maintenance job and part,
     * oldest first — used by MaintenanceRapportService to consume against
     * the right reservation(s) when a technician records actual usage.
     */
    @Transactional(readOnly = true)
    public List<PartReservation> findActiveReservations(Long maintenanceId, Long partId) {
        if (maintenanceId == null || partId == null) {
            return List.of();
        }
        return reservationRepository.findByMaintenanceId(maintenanceId).stream()
                .filter(r -> partId.equals(r.getPartId()) && r.getStatus() == PartReservationStatus.RESERVED)
                .sorted((a, b) -> a.getReservedAt().compareTo(b.getReservedAt()))
                .toList();
    }

    /** Releases every still-active (RESERVED) reservation for a job — used when a maintenance task is cancelled/deleted. */
    public void releaseAllForMaintenance(Long maintenanceId) {
        reservationRepository.findByMaintenanceId(maintenanceId).stream()
                .filter(r -> r.getStatus() == PartReservationStatus.RESERVED)
                .forEach(r -> {
                    r.setStatus(PartReservationStatus.RELEASED);
                    r.setResolvedAt(LocalDateTime.now());
                    reservationRepository.save(r);
                });
    }

    @Transactional(readOnly = true)
    public List<PartReservationResponse> byPart(Long partId) {
        return reservationRepository.findByPartIdAndStatus(partId, PartReservationStatus.RESERVED).stream()
                .map(r -> toResponse(r, partName(r.getPartId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PartReservationResponse> byMaintenance(Long maintenanceId) {
        return reservationRepository.findByMaintenanceId(maintenanceId).stream()
                .map(r -> toResponse(r, partName(r.getPartId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public int availableStock(Part part) {
        Integer reserved = reservationRepository.sumReservedQuantity(part.getId());
        return part.getCurrentStock() - (reserved == null ? 0 : reserved);
    }

    /** partId -> total quantity currently held in active reservations. Parts with none aren't present in the map. */
    @Transactional(readOnly = true)
    public Map<Long, Integer> getReservedQuantitiesByPart() {
        return reservationRepository.sumReservedQuantityByPart().stream()
                .collect(Collectors.toMap(
                        PartReservationRepository.PartIdAndReserved::getPartId,
                        PartReservationRepository.PartIdAndReserved::getTotal));
    }

    private PartReservation getActiveOrThrow(Long id) {
        PartReservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found: " + id));
        if (reservation.getStatus() != PartReservationStatus.RESERVED) {
            throw new IllegalStateException("Reservation " + id + " is already " + reservation.getStatus());
        }
        return reservation;
    }

    private String partName(Long partId) {
        return partRepository.findById(partId).map(Part::getName).orElse(null);
    }

    private PartReservationResponse toResponse(PartReservation reservation, String partName) {
        return PartReservationResponse.builder()
                .id(reservation.getId())
                .partId(reservation.getPartId())
                .partName(partName)
                .quantityReserved(reservation.getQuantityReserved())
                .quantityConsumed(reservation.getQuantityConsumed())
                .maintenanceId(reservation.getMaintenanceId())
                .status(reservation.getStatus())
                .reservedBy(reservation.getReservedBy())
                .reservedAt(reservation.getReservedAt())
                .resolvedAt(reservation.getResolvedAt())
                .build();
    }
}
