package com.pfe.predictive.inventory.service;

import com.pfe.predictive.inventory.dto.LowStockAlertResponse;
import com.pfe.predictive.inventory.dto.PartRequest;
import com.pfe.predictive.inventory.dto.PartResponse;
import com.pfe.predictive.inventory.dto.PartUpdateRequest;
import com.pfe.predictive.inventory.entity.Part;
import com.pfe.predictive.inventory.entity.PartStatus;
import com.pfe.predictive.inventory.mapper.PartMapper;
import com.pfe.predictive.inventory.repository.PartRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PartService {

    private final PartRepository partRepository;
    private final PartMapper partMapper;
    private final com.pfe.predictive.inventory.service.CategoryService categoryService;

    public PartResponse createPart(PartRequest request) {
        log.info("Creating part: {}", request.getName());
        // Validate category exists when provided
        String cat = request.getCategory();
        if (cat != null && !cat.isBlank() && !categoryService.existsByName(cat.trim())) {
            throw new IllegalArgumentException("Category not found: " + cat);
        }

        Part part = partMapper.toEntity(request);

        // Determine current stock (use provided value or default 0)
        int currentStock = request.getCurrentStock() != null ? request.getCurrentStock() : 0;
        part.setCurrentStock(currentStock);

        // Determine minimum stock (use provided or default 0)
        int minStock = request.getMinimumStock() != null ? request.getMinimumStock() : 0;
        part.setMinimumStock(minStock);

        // Set initial status based on stock levels
        if (currentStock <= 0) {
            part.setStatus(com.pfe.predictive.inventory.entity.PartStatus.OUT_OF_STOCK);
        } else if (currentStock <= minStock) {
            part.setStatus(com.pfe.predictive.inventory.entity.PartStatus.LOW_STOCK);
        } else {
            part.setStatus(com.pfe.predictive.inventory.entity.PartStatus.AVAILABLE);
        }

        Part savedPart = partRepository.save(part);
        return partMapper.toResponse(savedPart);
    }

    @Transactional(readOnly = true)
    public PartResponse getPartById(Long id) {
        Part part = partRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Part not found: " + id));
        return partMapper.toResponse(part);
    }

    @Transactional(readOnly = true)
    public List<PartResponse> getAllParts() {
        return partRepository.findAll()
            .stream()
            .map(partMapper::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<PartResponse> getAllPartsPaginated(org.springframework.data.domain.Pageable pageable) {
        return partRepository.findAll(pageable)
                .map(partMapper::toResponse);
    }

    public PartResponse updatePart(Long id, PartUpdateRequest request) {
        log.info("Updating part: {}", id);

        Part part = partRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Part not found: " + id));

        if (request.getName() != null) {
            part.setName(request.getName());
        }
        if (request.getDescription() != null) {
            part.setDescription(request.getDescription());
        }
        if (request.getCost() != null) {
            part.setCost(request.getCost());
        }
        if (request.getMinimumStock() != null) {
            part.setMinimumStock(request.getMinimumStock());
        }
        if (request.getReorderQuantity() != null) {
            part.setReorderQuantity(request.getReorderQuantity());
        }
        if (request.getSupplier() != null) {
            part.setSupplier(request.getSupplier());
        }

        Part updated = partRepository.save(part);
        return partMapper.toResponse(updated);
    }

    public void deletePart(Long id) {
        log.info("Deleting part: {}", id);
        partRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<LowStockAlertResponse> getLowStockParts() {
        return partRepository.findLowStockParts()
            .stream()
            .map(part -> LowStockAlertResponse.builder()
                .partId(part.getId())
                .partName(part.getName())
                .currentStock(part.getCurrentStock())
                .minimumStock(part.getMinimumStock())
                .reorderQuantity(part.getReorderQuantity())
                .status(part.getStatus().toString())
                .build())
            .toList();
    }

    @Transactional(readOnly = true)
    public List<String> getCategories() {
        return partRepository.findDistinctCategories();
    }

    public void updateStockAfterUsage(Long partId, Integer quantityUsed) {
        log.info("Reducing stock for part {}: -{}", partId, quantityUsed);

        Part part = partRepository.findById(partId)
            .orElseThrow(() -> new IllegalArgumentException("Part not found: " + partId));

        int newStock = part.getCurrentStock() - quantityUsed;
        if (newStock < 0) {
            throw new IllegalArgumentException("Insufficient stock for part: " + partId);
        }

        part.setCurrentStock(newStock);

        if (newStock == 0) {
            part.setStatus(PartStatus.OUT_OF_STOCK);
        } else if (newStock <= part.getMinimumStock()) {
            part.setStatus(PartStatus.LOW_STOCK);
        } else {
            part.setStatus(PartStatus.AVAILABLE);
        }

        partRepository.save(part);
    }

    public void updateStockAfterReceipt(Long partId, Integer quantityReceived) {
        log.info("Adding stock for part {}: +{}", partId, quantityReceived);

        Part part = partRepository.findById(partId)
            .orElseThrow(() -> new IllegalArgumentException("Part not found: " + partId));

        part.setCurrentStock(part.getCurrentStock() + quantityReceived);
        part.setStatus(PartStatus.AVAILABLE);
        partRepository.save(part);
    }
}
