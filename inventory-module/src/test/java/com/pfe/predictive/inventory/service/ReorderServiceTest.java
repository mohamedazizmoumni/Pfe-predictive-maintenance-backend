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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReorderServiceTest {

    @Mock
    private ReorderRequestRepository reorderRepository;

    @Mock
    private PartRepository partRepository;

    private ReorderService service;

    @BeforeEach
    void setUp() {
        service = new ReorderService(reorderRepository, partRepository, new ReorderMapper());
    }

    private ReorderRequest reorder(ReorderStatus status) {
        Part part = Part.builder().id(1L).name("Bearing").partNumber("PN-1").build();
        return ReorderRequest.builder()
                .id(10L)
                .part(part)
                .quantity(50)
                .status(status)
                .requestedBy("stock.mgr")
                .build();
    }

    @Test
    void requestReorderThrowsWhenPartMissing() {
        when(partRepository.findById(99L)).thenReturn(Optional.empty());

        ReorderRequestRequest request = ReorderRequestRequest.builder().partId(99L).quantity(10).build();

        assertThrows(IllegalArgumentException.class, () -> service.requestReorder(request, "stock.mgr"));
        verify(reorderRepository, never()).save(any());
    }

    @Test
    void approveReorderTransitionsToApprovedAndStampsApprover() {
        ReorderRequest reorder = reorder(ReorderStatus.REQUESTED);
        when(reorderRepository.findById(10L)).thenReturn(Optional.of(reorder));
        when(reorderRepository.save(any(ReorderRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        ReorderRequestResponse response = service.approveReorder(
                10L, ReorderApprovalRequest.builder().approved(true).build(), "mgr.paul");

        assertEquals(ReorderStatus.APPROVED.toString(), response.getStatus());
        assertEquals("mgr.paul", reorder.getApprovedBy());
    }

    @Test
    void approveReorderRejectionClearsApproverFields() {
        ReorderRequest reorder = reorder(ReorderStatus.REQUESTED);
        when(reorderRepository.findById(10L)).thenReturn(Optional.of(reorder));
        when(reorderRepository.save(any(ReorderRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        ReorderRequestResponse response = service.approveReorder(
                10L, ReorderApprovalRequest.builder().approved(false).build(), "mgr.paul");

        assertEquals(ReorderStatus.REJECTED.toString(), response.getStatus());
        assertNull(reorder.getApprovedBy());
        assertNull(reorder.getApprovedDate());
    }

    @Test
    void approveReorderRejectsAlreadyDecidedRequest() {
        ReorderRequest reorder = reorder(ReorderStatus.APPROVED);
        when(reorderRepository.findById(10L)).thenReturn(Optional.of(reorder));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.approveReorder(10L, ReorderApprovalRequest.builder().approved(true).build(), "mgr.paul"));

        assertEquals(true, ex.getMessage().contains("cannot be approved/rejected"));
        verify(reorderRepository, never()).save(any());
    }

    @Test
    void getReorderByIdThrowsWhenMissing() {
        when(reorderRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.getReorderById(404L));
    }
}
