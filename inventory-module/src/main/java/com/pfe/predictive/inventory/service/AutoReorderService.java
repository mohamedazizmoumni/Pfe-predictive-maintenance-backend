package com.pfe.predictive.inventory.service;

import com.pfe.predictive.inventory.dto.ReorderRequestRequest;
import com.pfe.predictive.inventory.entity.Part;
import com.pfe.predictive.inventory.entity.ReorderStatus;
import com.pfe.predictive.inventory.repository.PartRepository;
import com.pfe.predictive.inventory.repository.ReorderRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implements the automatic-reorder behavior inventory.low-stock-threshold-percentage
 * previously configured but nothing ever read (functional audit, business-logic
 * item #6). Off by default — flip inventory.auto-reorder-enabled to true to
 * activate. Drafts a REQUESTED reorder for Stock Manager/Finance to review
 * through the normal approval flow — never auto-purchases anything.
 *
 * Skips any part that already has a pending (REQUESTED) reorder, so this can
 * run on a schedule indefinitely without drafting duplicates while a part
 * stays below the trigger level waiting for approval.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AutoReorderService {

    private static final String SYSTEM_ACTOR = "SYSTEM";

    private final PartRepository partRepository;
    private final ReorderRequestRepository reorderRequestRepository;
    private final ReorderService reorderService;

    @Value("${inventory.auto-reorder-enabled:false}")
    private boolean autoReorderEnabled;

    @Value("${inventory.low-stock-threshold-percentage:20}")
    private double lowStockThresholdPercentage;

    @Transactional
    public void draftReordersForPartsBelowTrigger() {
        if (!autoReorderEnabled) {
            return;
        }

        List<Part> candidates = partRepository.findPartsAtOrBelowReorderTrigger(lowStockThresholdPercentage);
        int drafted = 0;

        for (Part part : candidates) {
            if (reorderRequestRepository.existsByPartIdAndStatus(part.getId(), ReorderStatus.REQUESTED)) {
                continue; // already has a pending request awaiting review
            }

            int quantity = (part.getReorderQuantity() != null && part.getReorderQuantity() > 0)
                    ? part.getReorderQuantity()
                    : Math.max(1, part.getMinimumStock());

            ReorderRequestRequest request = ReorderRequestRequest.builder()
                    .partId(part.getId())
                    .quantity(quantity)
                    .reason("Automatic — stock (" + part.getCurrentStock() + ") at or below the "
                            + lowStockThresholdPercentage + "% reorder trigger over minimum ("
                            + part.getMinimumStock() + ")")
                    .requestedBy(SYSTEM_ACTOR)
                    .build();

            reorderService.requestReorder(request, SYSTEM_ACTOR);
            drafted++;
        }

        if (drafted > 0) {
            log.info("Auto-reorder pass: drafted {} reorder request(s) for review", drafted);
        }
    }
}
