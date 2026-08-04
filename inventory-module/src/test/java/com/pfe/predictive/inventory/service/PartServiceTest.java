package com.pfe.predictive.inventory.service;

import com.pfe.predictive.inventory.dto.LowStockAlertResponse;
import com.pfe.predictive.inventory.dto.PartRequest;
import com.pfe.predictive.inventory.dto.PartResponse;
import com.pfe.predictive.inventory.dto.PartUpdateRequest;
import com.pfe.predictive.inventory.entity.Part;
import com.pfe.predictive.inventory.entity.PartStatus;
import com.pfe.predictive.inventory.mapper.PartMapper;
import com.pfe.predictive.inventory.repository.PartRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers PartService's stock-status derivation rules (the same thresholds
 * are recomputed in three different places: create, update, usage/receipt)
 * and the guard clauses around part-number uniqueness, category existence,
 * insufficient stock, and image upload validation.
 */
@ExtendWith(MockitoExtension.class)
class PartServiceTest {

    @Mock
    private PartRepository partRepository;

    @Mock
    private CategoryService categoryService;

    private PartService service;

    @BeforeEach
    void setUp() {
        // Real mapper: pure field mapping, mocking it would just restate the assertions.
        service = new PartService(partRepository, new PartMapper(), categoryService);
    }

    private Part existingPart(int currentStock, int minimumStock) {
        return Part.builder()
                .id(1L)
                .partNumber("PN-1")
                .name("Bearing")
                .currentStock(currentStock)
                .minimumStock(minimumStock)
                .status(PartStatus.AVAILABLE)
                .build();
    }

    // ------------------------------------------------------------------
    // createPart
    // ------------------------------------------------------------------

    @Test
    void createPartRejectsUnknownCategory() {
        PartRequest request = PartRequest.builder().name("Bearing").category("Bogus").build();
        when(categoryService.existsByName("Bogus")).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.createPart(request));

        assertEquals("Category not found: Bogus", ex.getMessage());
        verify(partRepository, never()).save(any());
    }

    @Test
    void createPartRejectsDuplicatePartNumber() {
        PartRequest request = PartRequest.builder().name("Bearing").partNumber("PN-1").build();
        when(partRepository.existsByPartNumber("PN-1")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.createPart(request));

        assertEquals("A part with part number 'PN-1' already exists", ex.getMessage());
        verify(partRepository, never()).save(any());
    }

    @ParameterizedTest
    @CsvSource({
            "0, 5, OUT_OF_STOCK",
            "3, 5, LOW_STOCK",
            "5, 5, LOW_STOCK",
            "6, 5, AVAILABLE",
    })
    void createPartDerivesStatusFromStockThresholds(int currentStock, int minStock, PartStatus expected) {
        PartRequest request = PartRequest.builder()
                .name("Bearing")
                .currentStock(currentStock)
                .minimumStock(minStock)
                .build();
        when(partRepository.save(any(Part.class))).thenAnswer(inv -> inv.getArgument(0));

        PartResponse response = service.createPart(request);

        assertEquals(expected.toString(), response.getStatus());
    }

    @Test
    void createPartDefaultsStockAndMinimumToZeroWhenOmitted() {
        PartRequest request = PartRequest.builder().name("Bearing").build();
        when(partRepository.save(any(Part.class))).thenAnswer(inv -> inv.getArgument(0));

        PartResponse response = service.createPart(request);

        assertEquals(0, response.getCurrentStock());
        assertEquals(0, response.getMinimumStock());
        assertEquals(PartStatus.OUT_OF_STOCK.toString(), response.getStatus());
    }

    // ------------------------------------------------------------------
    // updatePart
    // ------------------------------------------------------------------

    @Test
    void updatePartOnlyTouchesProvidedFields() {
        Part part = existingPart(10, 5);
        part.setDescription("Original description");
        when(partRepository.findById(1L)).thenReturn(Optional.of(part));
        when(partRepository.save(any(Part.class))).thenAnswer(inv -> inv.getArgument(0));

        PartUpdateRequest request = PartUpdateRequest.builder().cost(new BigDecimal("42.50")).build();
        PartResponse response = service.updatePart(1L, request);

        assertEquals("Original description", response.getDescription());
        assertEquals(new BigDecimal("42.50"), response.getCost());
    }

    @Test
    void updatePartRejectsPartNumberCollisionWithAnotherPart() {
        Part part = existingPart(10, 5);
        when(partRepository.findById(1L)).thenReturn(Optional.of(part));
        when(partRepository.existsByPartNumberAndIdNot("PN-2", 1L)).thenReturn(true);

        PartUpdateRequest request = PartUpdateRequest.builder().partNumber("PN-2").build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.updatePart(1L, request));

        assertEquals("A part with part number 'PN-2' already exists", ex.getMessage());
        verify(partRepository, never()).save(any());
    }

    @Test
    void updatePartAllowsKeepingTheSamePartNumber() {
        Part part = existingPart(10, 5);
        when(partRepository.findById(1L)).thenReturn(Optional.of(part));
        when(partRepository.save(any(Part.class))).thenAnswer(inv -> inv.getArgument(0));

        PartUpdateRequest request = PartUpdateRequest.builder().partNumber("PN-1").build();
        service.updatePart(1L, request);

        verify(partRepository, never()).existsByPartNumberAndIdNot(anyString(), anyLong());
    }

    @Test
    void updatePartRecomputesStatusWhenStockChanges() {
        Part part = existingPart(10, 5);
        when(partRepository.findById(1L)).thenReturn(Optional.of(part));
        when(partRepository.save(any(Part.class))).thenAnswer(inv -> inv.getArgument(0));

        PartUpdateRequest request = PartUpdateRequest.builder().currentStock(2).build();
        PartResponse response = service.updatePart(1L, request);

        assertEquals(PartStatus.LOW_STOCK.toString(), response.getStatus());
    }

    @Test
    void updatePartThrowsWhenPartMissing() {
        when(partRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> service.updatePart(99L, PartUpdateRequest.builder().build()));
    }

    // ------------------------------------------------------------------
    // updateStockAfterUsage / updateStockAfterReceipt
    // ------------------------------------------------------------------

    @Test
    void updateStockAfterUsageRejectsInsufficientStock() {
        Part part = existingPart(3, 5);
        when(partRepository.findById(1L)).thenReturn(Optional.of(part));

        assertThrows(IllegalArgumentException.class, () -> service.updateStockAfterUsage(1L, 5));
        verify(partRepository, never()).save(any());
    }

    @ParameterizedTest
    @CsvSource({
            "10, 5, 10, OUT_OF_STOCK",  // exact depletion
            "10, 5, 6, LOW_STOCK",      // leaves stock at/under minimum
            "10, 2, 3, AVAILABLE",      // leaves stock comfortably above minimum
    })
    void updateStockAfterUsageRecomputesStatus(int startStock, int minStock, int used, PartStatus expected) {
        Part part = existingPart(startStock, minStock);
        when(partRepository.findById(1L)).thenReturn(Optional.of(part));

        service.updateStockAfterUsage(1L, used);

        assertEquals(expected, part.getStatus());
        assertEquals(startStock - used, part.getCurrentStock());
    }

    @Test
    void updateStockAfterReceiptAddsQuantityAndMarksAvailable() {
        Part part = existingPart(0, 5);
        part.setStatus(PartStatus.OUT_OF_STOCK);
        when(partRepository.findById(1L)).thenReturn(Optional.of(part));

        service.updateStockAfterReceipt(1L, 20);

        assertEquals(20, part.getCurrentStock());
        assertEquals(PartStatus.AVAILABLE, part.getStatus());
    }

    // ------------------------------------------------------------------
    // getLowStockParts
    // ------------------------------------------------------------------

    @Test
    void getLowStockPartsMapsToAlertResponses() {
        Part part = existingPart(2, 5);
        part.setReorderQuantity(15);
        part.setStatus(PartStatus.LOW_STOCK);
        when(partRepository.findLowStockParts()).thenReturn(List.of(part));

        List<LowStockAlertResponse> alerts = service.getLowStockParts();

        assertEquals(1, alerts.size());
        assertEquals(1L, alerts.get(0).getPartId());
        assertEquals(2, alerts.get(0).getCurrentStock());
        assertEquals(15, alerts.get(0).getReorderQuantity());
        assertEquals("LOW_STOCK", alerts.get(0).getStatus());
    }

    // ------------------------------------------------------------------
    // uploadPartImage validation guards
    // ------------------------------------------------------------------

    @Test
    void uploadPartImageRejectsEmptyFile() {
        when(partRepository.findById(1L)).thenReturn(Optional.of(existingPart(10, 5)));
        MockMultipartFile empty = new MockMultipartFile("image", "a.png", "image/png", new byte[0]);

        assertThrows(IllegalArgumentException.class, () -> service.uploadPartImage(1L, empty));
    }

    @Test
    void uploadPartImageRejectsNonImageContentType() {
        when(partRepository.findById(1L)).thenReturn(Optional.of(existingPart(10, 5)));
        MockMultipartFile pdf = new MockMultipartFile("image", "a.pdf", "application/pdf", new byte[]{1, 2, 3});

        assertThrows(IllegalArgumentException.class, () -> service.uploadPartImage(1L, pdf));
    }

    @Test
    void uploadPartImageRejectsOversizedFile() {
        when(partRepository.findById(1L)).thenReturn(Optional.of(existingPart(10, 5)));
        byte[] oversized = new byte[6 * 1024 * 1024];
        MockMultipartFile tooBig = new MockMultipartFile("image", "a.png", "image/png", oversized);

        assertThrows(IllegalArgumentException.class, () -> service.uploadPartImage(1L, tooBig));
    }

    // ------------------------------------------------------------------
    // deletePartImage
    // ------------------------------------------------------------------

    @Test
    void deletePartImageIsNoOpWhenPartHasNoImage() {
        Part part = existingPart(10, 5);
        part.setImageUrl(null);
        when(partRepository.findById(1L)).thenReturn(Optional.of(part));

        service.deletePartImage(1L);

        verify(partRepository, never()).save(any());
    }
}
