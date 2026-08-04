package com.pfe.predictive.inventory.service;

import com.pfe.predictive.inventory.dto.ReorderApprovalRequest;
import com.pfe.predictive.inventory.dto.ReorderRequestRequest;
import com.pfe.predictive.inventory.dto.ReorderRequestResponse;
import com.pfe.predictive.inventory.entity.Part;
import com.pfe.predictive.inventory.entity.ReorderRequest;
import com.pfe.predictive.inventory.entity.ReorderStatus;
import com.pfe.predictive.inventory.mapper.ReorderMapper;
import com.pfe.predictive.inventory.repository.PartRepository;
import com.pfe.predictive.inventory.repository.ReorderRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ReorderService {

    // getAllReorders() has no client-driven paging yet - cap at a generous
    // size (most recent first) instead of loading the entire reorder history.
    private static final int LIST_CAP = 300;

    private final ReorderRequestRepository reorderRepository;
    private final PartRepository partRepository;
    private final ReorderMapper reorderMapper;

    public ReorderRequestResponse requestReorder(ReorderRequestRequest request, String requestedBy) {
        log.info("Creating reorder request for part: {}", request.getPartId());

        Part part = partRepository.findById(request.getPartId())
            .orElseThrow(() -> new IllegalArgumentException("Part not found: " + request.getPartId()));

        ReorderRequest reorder = reorderMapper.toEntity(request, part, requestedBy);
        ReorderRequest saved = reorderRepository.save(reorder);
        return reorderMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ReorderRequestResponse> getAllReorders() {
        return reorderRepository.findAll(PageRequest.of(0, LIST_CAP, Sort.by(Sort.Direction.DESC, "createdDate")))
            .getContent()
            .stream()
            .map(reorderMapper::toResponse)
            .toList();
    }

    public ReorderRequestResponse approveReorder(Long id, ReorderApprovalRequest request, String approvedBy) {
        log.info("Processing reorder approval: {} - Approved: {}", id, request.getApproved());

        ReorderRequest reorder = reorderRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Reorder not found: " + id));

        if (reorder.getStatus() != ReorderStatus.REQUESTED) {
            throw new IllegalStateException(
                "Reorder request " + id + " cannot be approved/rejected — current status is " + reorder.getStatus());
        }

        if (Boolean.TRUE.equals(request.getApproved())) {
            reorder.setStatus(ReorderStatus.APPROVED);
            reorder.setApprovedBy(approvedBy);
            reorder.setApprovedDate(LocalDateTime.now());
        } else {
            reorder.setStatus(ReorderStatus.REJECTED);
            reorder.setApprovedBy(null);
            reorder.setApprovedDate(null);
        }

        ReorderRequest updated = reorderRepository.save(reorder);
        return reorderMapper.toResponse(updated);
    }

    @Transactional(readOnly = true)
    public List<ReorderRequestResponse> getPendingReorders() {
        return reorderRepository.findByStatus(ReorderStatus.REQUESTED)
            .stream()
            .map(reorderMapper::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public ReorderRequestResponse getReorderById(Long id) {
        ReorderRequest reorder = reorderRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Reorder not found: " + id));
        return reorderMapper.toResponse(reorder);
    }
}