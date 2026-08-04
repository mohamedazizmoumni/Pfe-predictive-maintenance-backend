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
        PartReservation reservation = getActiveOrThrow(id);
        Part part = partRepository.findById(reservation.getPartId())
                .orElseThrow(() -> new IllegalArgumentException("Part not found: " + reservation.getPartId()));

        part.setCurrentStock(Math.max(0, part.getCurrentStock() - reservation.getQuantityReserved()));
        partRepository.save(part);

        reservation.setStatus(PartReservationStatus.CONSUMED);
        reservation.setResolvedAt(LocalDateTime.now());
        return toResponse(reservationRepository.save(reservation), part.getName());
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
                .maintenanceId(reservation.getMaintenanceId())
                .status(reservation.getStatus())
                .reservedBy(reservation.getReservedBy())
                .reservedAt(reservation.getReservedAt())
                .resolvedAt(reservation.getResolvedAt())
                .build();
    }
}
