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

        String partNumber = request.getPartNumber();
        if (partNumber != null && !partNumber.isBlank() && partRepository.existsByPartNumber(partNumber.trim())) {
            throw new IllegalArgumentException("A part with part number '" + partNumber.trim() + "' already exists");
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
        if (request.getCategory() != null) {
            part.setCategory(request.getCategory());
        }
        if (request.getSubCategory() != null) {
            part.setSubCategory(request.getSubCategory());
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
        if (request.getPartNumber() != null) {
            String newPartNumber = request.getPartNumber().trim();
            if (!newPartNumber.equals(part.getPartNumber())
                    && partRepository.existsByPartNumberAndIdNot(newPartNumber, id)) {
                throw new IllegalArgumentException("A part with part number '" + newPartNumber + "' already exists");
            }
            part.setPartNumber(newPartNumber);
        }
        if (request.getUnit() != null) {
            part.setUnit(request.getUnit());
        }
        if (request.getNotes() != null) {
            part.setNotes(request.getNotes());
        }
        if (request.getCurrentStock() != null) {
            part.setCurrentStock(request.getCurrentStock());
            
            // Update status based on new stock level
            int currentStock = request.getCurrentStock();
            int minStock = part.getMinimumStock() != null ? part.getMinimumStock() : 0;
            
            if (currentStock <= 0) {
                part.setStatus(PartStatus.OUT_OF_STOCK);
            } else if (currentStock <= minStock) {
                part.setStatus(PartStatus.LOW_STOCK);
            } else {
                part.setStatus(PartStatus.AVAILABLE);
            }
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

    @Transactional(readOnly = true)
    public List<PartResponse> getPartsByCategory(String category) {
        return partRepository.findByCategory(category)
            .stream()
            .map(partMapper::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<PartResponse> getPartsBySubCategory(String subCategory) {
        return partRepository.findBySubCategory(subCategory)
            .stream()
            .map(partMapper::toResponse)
            .toList();
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
    
    /**
     * Upload part image
     */
    public PartResponse uploadPartImage(Long partId, org.springframework.web.multipart.MultipartFile image) {
        log.info("Uploading image for part: {}", partId);
        
        Part part = partRepository.findById(partId)
            .orElseThrow(() -> new IllegalArgumentException("Part not found: " + partId));
        
        // Validate file
        if (image.isEmpty()) {
            throw new IllegalArgumentException("Image file is empty");
        }
        
        // Validate file type
        String contentType = image.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("File must be an image");
        }
        
        // Validate file size (max 5MB)
        long maxSize = 5L * 1024 * 1024; // 5MB
        if (image.getSize() > maxSize) {
            throw new IllegalArgumentException("Image size must not exceed 5MB");
        }
        
        try {
            // Create uploads directory if it doesn't exist.
            // Must be an absolute path: MultipartFile.transferTo(File) resolves relative
            // paths against the servlet container's internal temp dir, not the app's
            // working directory, which caused writes to silently target the wrong folder.
            java.nio.file.Path uploadDir = java.nio.file.Paths.get("uploads/parts").toAbsolutePath();
            if (!java.nio.file.Files.exists(uploadDir)) {
                java.nio.file.Files.createDirectories(uploadDir);
            }
            
            // Generate unique filename
            String originalFilename = image.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".") 
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : ".jpg";
            String filename = "part_" + partId + "_" + System.currentTimeMillis() + extension;
            
            // Save file
            java.nio.file.Path filePath = uploadDir.resolve(filename);
            image.transferTo(filePath.toFile());
            
            // Update part with image URL
            String imageUrl = "/uploads/parts/" + filename;
            part.setImageUrl(imageUrl);
            Part saved = partRepository.save(part);
            
            log.info("Image uploaded successfully for part {}: {}", partId, imageUrl);
            return partMapper.toResponse(saved);
            
        } catch (java.io.IOException e) {
            log.error("Failed to upload image for part {}: {}", partId, e.getMessage(), e);
            throw new IllegalArgumentException("Failed to upload image: " + e.getMessage());
        }
    }
    
    /**
     * Delete part image
     */
    public PartResponse deletePartImage(Long partId) {
        log.info("Deleting image for part: {}", partId);
        
        Part part = partRepository.findById(partId)
            .orElseThrow(() -> new IllegalArgumentException("Part not found: " + partId));
        
        if (part.getImageUrl() != null) {
            try {
                // Delete physical file
                String imageUrl = part.getImageUrl();
                if (imageUrl.startsWith("/uploads/")) {
                    java.nio.file.Path filePath = java.nio.file.Paths.get(imageUrl.substring(1));
                    java.nio.file.Files.deleteIfExists(filePath);
                }
            } catch (java.io.IOException e) {
                log.warn("Failed to delete image file for part {}: {}", partId, e.getMessage());
            }
            
            // Remove image URL from database
            part.setImageUrl(null);
            Part saved = partRepository.save(part);
            
            log.info("Image deleted successfully for part {}", partId);
            return partMapper.toResponse(saved);
        }
        
        return partMapper.toResponse(part);
    }
}
