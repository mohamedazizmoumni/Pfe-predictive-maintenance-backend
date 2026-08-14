package com.pfe.predictive.alert.service;

import com.pfe.predictive.alert.entity.Alert;
import com.pfe.predictive.alert.entity.AlertStatus;
import com.pfe.predictive.alert.repository.AlertRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Covers getAverageResolutionTimeHours() - added in the same change as the
 * severity-threshold externalization (AlertDecisionEngine), no prior test
 * exercised this method at all.
 */
@ExtendWith(MockitoExtension.class)
class AlertQueryServiceTest {

    @Mock
    private AlertRepository alertRepository;

    private AlertQueryService service;

    @BeforeEach
    void setUp() {
        service = new AlertQueryService(alertRepository);
    }

    private Alert closedAlert(LocalDateTime created, LocalDateTime closed) {
        return Alert.builder()
                .id(1L)
                .machineId(42L)
                .title("Press-14 degrading")
                .message("Health dropped")
                .status(AlertStatus.CLOSED)
                .createdDate(created)
                .closedDate(closed)
                .build();
    }

    @Test
    void averageResolutionTimeIsNullWhenNoAlertsHaveBeenClosed() {
        when(alertRepository.findByStatus(eq(AlertStatus.CLOSED), any(Pageable.class)))
                .thenReturn(Page.empty());

        assertNull(service.getAverageResolutionTimeHours());
    }

    @Test
    void averageResolutionTimeIgnoresAlertsMissingTimestamps() {
        Alert missingClosedDate = closedAlert(LocalDateTime.now(), null);
        Alert missingCreatedDate = closedAlert(null, LocalDateTime.now());

        when(alertRepository.findByStatus(eq(AlertStatus.CLOSED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(missingClosedDate, missingCreatedDate)));

        assertNull(service.getAverageResolutionTimeHours());
    }

    @Test
    void averageResolutionTimeIsComputedInHoursFromCreatedToClosedTimestamps() {
        LocalDateTime start = LocalDateTime.of(2026, 8, 1, 8, 0);
        // Two alerts: one resolved in 2 hours, one in 4 hours -> average 3.0h.
        Alert fast = closedAlert(start, start.plusHours(2));
        Alert slow = closedAlert(start, start.plusHours(4));

        when(alertRepository.findByStatus(eq(AlertStatus.CLOSED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(fast, slow)));

        assertEquals(3.0, service.getAverageResolutionTimeHours(), 0.0001);
    }
}
