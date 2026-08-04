package com.pfe.predictive.alert.service;

import com.pfe.predictive.alert.dto.AcknowledgeAlertRequest;
import com.pfe.predictive.alert.dto.CloseAlertRequest;
import com.pfe.predictive.alert.dto.CreateAlertRequest;
import com.pfe.predictive.alert.dto.EscalateAlertRequest;
import com.pfe.predictive.alert.entity.Alert;
import com.pfe.predictive.alert.entity.AlertSeverity;
import com.pfe.predictive.alert.entity.AlertStatus;
import com.pfe.predictive.alert.mapper.AlertMapper;
import com.pfe.predictive.alert.repository.AlertRepository;
import com.pfe.predictive.audit.service.AuditEventService;
import com.pfe.predictive.common.service.EmailService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers the alert lifecycle state-machine business rules in AlertService:
 * NEW -> ACKNOWLEDGED -> ESCALATED -> CLOSED, and the guards that reject
 * invalid transitions (e.g. escalating a closed alert).
 */
@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private AlertNotificationProperties alertNotificationProperties;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private AuditEventService auditEventService;

    private AlertService service;

    @BeforeEach
    void setUp() {
        // Real mapper: it's pure field-mapping logic, mocking it would just
        // re-describe the assertions instead of exercising them.
        service = new AlertService(alertRepository, new AlertMapper(), emailService,
                alertNotificationProperties, messagingTemplate, auditEventService);
    }

    private Alert newAlert(AlertStatus status) {
        return Alert.builder()
                .id(1L)
                .machineId(42L)
                .title("Press-14 degrading")
                .message("Health dropped")
                .severity(AlertSeverity.WARNING)
                .status(status)
                .viewed(false)
                .isActive(true)
                .build();
    }

    // ------------------------------------------------------------------
    // acknowledgeAlert: NEW -> ACKNOWLEDGED
    // ------------------------------------------------------------------

    @Test
    void acknowledgeFromNewSucceeds() {
        Alert alert = newAlert(AlertStatus.NEW);
        when(alertRepository.findById(1L)).thenReturn(Optional.of(alert));
        when(alertRepository.save(any(Alert.class))).thenAnswer(inv -> inv.getArgument(0));

        Alert result = service.acknowledgeAlert(1L, "tech.jane", AcknowledgeAlertRequest.builder().build());

        assertEquals(AlertStatus.ACKNOWLEDGED, result.getStatus());
        assertEquals("tech.jane", result.getAcknowledgedBy());
        assertNotNull(result.getAcknowledgedDate());
        assertTrue(result.getViewed());
    }

    @Test
    void acknowledgeFromNonNewStatusThrows() {
        Alert alert = newAlert(AlertStatus.ACKNOWLEDGED);
        when(alertRepository.findById(1L)).thenReturn(Optional.of(alert));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.acknowledgeAlert(1L, "tech.jane", AcknowledgeAlertRequest.builder().build()));

        assertTrue(ex.getMessage().contains("only be acknowledged from NEW status"));
        verify(alertRepository, never()).save(any());
    }

    @Test
    void acknowledgeUnknownAlertThrowsNotFound() {
        when(alertRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> service.acknowledgeAlert(99L, "tech.jane", AcknowledgeAlertRequest.builder().build()));
    }

    // ------------------------------------------------------------------
    // escalateAlert: ACKNOWLEDGED -> ESCALATED (NEW is also accepted and
    // auto-acknowledges on the way through)
    // ------------------------------------------------------------------

    @Test
    void escalateFromAcknowledgedSucceeds() {
        Alert alert = newAlert(AlertStatus.ACKNOWLEDGED);
        alert.setAcknowledgedBy("tech.jane");
        alert.setAcknowledgedDate(LocalDateTime.now().minusHours(1));
        when(alertRepository.findById(1L)).thenReturn(Optional.of(alert));
        when(alertRepository.save(any(Alert.class))).thenAnswer(inv -> inv.getArgument(0));

        EscalateAlertRequest request = EscalateAlertRequest.builder()
                .escalationNotes("Needs manager sign-off")
                .reassignTo("mgr.paul")
                .build();

        Alert result = service.escalateAlert(1L, "mgr.paul", request);

        assertEquals(AlertStatus.ESCALATED, result.getStatus());
        assertEquals("mgr.paul", result.getEscalatedBy());
        assertEquals("mgr.paul", result.getAssignedTo());
        assertEquals("Needs manager sign-off", result.getEscalationNotes());
        assertNotNull(result.getEscalatedDate());
        assertTrue(result.getViewed());
    }

    @Test
    void escalateFromNewAutoAcknowledgesFirst() {
        Alert alert = newAlert(AlertStatus.NEW);
        when(alertRepository.findById(1L)).thenReturn(Optional.of(alert));
        when(alertRepository.save(any(Alert.class))).thenAnswer(inv -> inv.getArgument(0));

        Alert result = service.escalateAlert(1L, "mgr.paul", EscalateAlertRequest.builder().build());

        assertEquals(AlertStatus.ESCALATED, result.getStatus());
        assertEquals("mgr.paul", result.getAcknowledgedBy());
        assertNotNull(result.getAcknowledgedDate());
    }

    @Test
    void escalateClosedAlertThrows() {
        Alert alert = newAlert(AlertStatus.CLOSED);
        when(alertRepository.findById(1L)).thenReturn(Optional.of(alert));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.escalateAlert(1L, "mgr.paul", EscalateAlertRequest.builder().build()));

        assertTrue(ex.getMessage().contains("Closed alerts cannot be escalated"));
        verify(alertRepository, never()).save(any());
    }

    @Test
    void escalateAlreadyEscalatedAlertThrows() {
        Alert alert = newAlert(AlertStatus.ESCALATED);
        when(alertRepository.findById(1L)).thenReturn(Optional.of(alert));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.escalateAlert(1L, "mgr.paul", EscalateAlertRequest.builder().build()));

        assertTrue(ex.getMessage().contains("already escalated"));
        verify(alertRepository, never()).save(any());
    }

    // ------------------------------------------------------------------
    // closeAlert: any non-CLOSED status -> CLOSED, requires resolution notes
    // ------------------------------------------------------------------

    @Test
    void closeEscalatedAlertSucceeds() {
        Alert alert = newAlert(AlertStatus.ESCALATED);
        when(alertRepository.findById(1L)).thenReturn(Optional.of(alert));
        when(alertRepository.save(any(Alert.class))).thenAnswer(inv -> inv.getArgument(0));

        Alert result = service.closeAlert(1L, "mgr.paul",
                CloseAlertRequest.builder().resolutionNotes("Bearing replaced").build());

        assertEquals(AlertStatus.CLOSED, result.getStatus());
        assertEquals("mgr.paul", result.getClosedBy());
        assertEquals("Bearing replaced", result.getResolutionNotes());
        assertFalse(result.getIsActive());
        assertNotNull(result.getClosedDate());
    }

    @Test
    void closeAlreadyClosedAlertThrows() {
        Alert alert = newAlert(AlertStatus.CLOSED);
        when(alertRepository.findById(1L)).thenReturn(Optional.of(alert));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.closeAlert(1L, "mgr.paul",
                        CloseAlertRequest.builder().resolutionNotes("n/a").build()));

        assertTrue(ex.getMessage().contains("already closed"));
        verify(alertRepository, never()).save(any());
    }

    // ------------------------------------------------------------------
    // openIncident: DB-level race guard (idx_alerts_active_incident_unique)
    // ------------------------------------------------------------------

    @Test
    void openIncidentReusesExistingIncidentWhenConstraintRejectsDuplicate() {
        Alert winner = newAlert(AlertStatus.NEW);
        winner.setIssueType("SENSOR_ANOMALY");

        when(alertRepository.save(any(Alert.class)))
                .thenThrow(new org.springframework.dao.DataIntegrityViolationException("duplicate key"));
        when(alertRepository.findByMachineIdAndIssueTypeAndIsActiveTrue(42L, "SENSOR_ANOMALY"))
                .thenReturn(Optional.of(winner));

        CreateAlertRequest request = CreateAlertRequest.builder()
                .machineId(42L)
                .title("Press-14 degrading")
                .severity(AlertSeverity.WARNING)
                .build();

        Alert result = service.openIncident(request, "SENSOR_ANOMALY", "SYSTEM_ML", AlertBroadcastContext.empty());

        assertEquals(winner, result);
        verify(emailService, never()).sendAlertNotification(any());
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    void openIncidentRethrowsWhenConstraintViolatedButNoActiveIncidentIsFound() {
        // Defensive case: the constraint fired but a concurrent close() also
        // raced in between — surface the original error rather than silently
        // returning null.
        org.springframework.dao.DataIntegrityViolationException original =
                new org.springframework.dao.DataIntegrityViolationException("duplicate key");
        when(alertRepository.save(any(Alert.class))).thenThrow(original);
        when(alertRepository.findByMachineIdAndIssueTypeAndIsActiveTrue(42L, "SENSOR_ANOMALY"))
                .thenReturn(Optional.empty());

        CreateAlertRequest request = CreateAlertRequest.builder()
                .machineId(42L)
                .title("Press-14 degrading")
                .severity(AlertSeverity.WARNING)
                .build();

        org.springframework.dao.DataIntegrityViolationException thrown = assertThrows(
                org.springframework.dao.DataIntegrityViolationException.class,
                () -> service.openIncident(request, "SENSOR_ANOMALY", "SYSTEM_ML", AlertBroadcastContext.empty()));

        assertEquals(original, thrown);
    }

    // ------------------------------------------------------------------
    // createAlert: email dispatch is gated by AlertNotificationProperties
    // ------------------------------------------------------------------

    @Test
    void createAlertSendsEmailWhenNotificationsEnabled() {
        when(alertNotificationProperties.isEnabled()).thenReturn(true);
        when(alertRepository.save(any(Alert.class))).thenAnswer(inv -> {
            Alert a = inv.getArgument(0);
            a.setId(1L);
            return a;
        });

        CreateAlertRequest request = CreateAlertRequest.builder()
                .machineId(42L)
                .title("Press-14 degrading")
                .severity(AlertSeverity.WARNING)
                .build();

        service.createAlert(request, "system");

        verify(emailService, times(1)).sendAlertNotification(any());
        verify(messagingTemplate, times(1)).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    void createAlertSkipsEmailWhenNotificationsDisabled() {
        when(alertNotificationProperties.isEnabled()).thenReturn(false);
        when(alertRepository.save(any(Alert.class))).thenAnswer(inv -> {
            Alert a = inv.getArgument(0);
            a.setId(1L);
            return a;
        });

        CreateAlertRequest request = CreateAlertRequest.builder()
                .machineId(42L)
                .title("Press-14 degrading")
                .severity(AlertSeverity.WARNING)
                .build();

        service.createAlert(request, "system");

        verify(emailService, never()).sendAlertNotification(any());
    }

    // ------------------------------------------------------------------
    // markAsViewed / getAlertById
    // ------------------------------------------------------------------

    @Test
    void markAsViewedIsNoOpWhenAlertMissing() {
        when(alertRepository.findById(99L)).thenReturn(Optional.empty());

        service.markAsViewed(99L);

        verify(alertRepository, never()).save(any());
    }

    @Test
    void markAsViewedSetsFlagWhenAlertExists() {
        Alert alert = newAlert(AlertStatus.NEW);
        when(alertRepository.findById(1L)).thenReturn(Optional.of(alert));

        service.markAsViewed(1L);

        assertTrue(alert.getViewed());
        verify(alertRepository, times(1)).save(alert);
    }

    @Test
    void getAlertByIdThrowsWhenMissing() {
        when(alertRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.getAlertById(1L));
    }
}
