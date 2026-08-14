package com.pfe.predictive.maintenance.service;

import com.pfe.predictive.audit.service.AuditEventService;
import com.pfe.predictive.common.service.EmailService;
import com.pfe.predictive.common.service.EscalationEmailContent;
import com.pfe.predictive.core.entity.Maintenance;
import com.pfe.predictive.core.entity.MaintenancePriority;
import com.pfe.predictive.core.entity.MaintenanceStatus;
import com.pfe.predictive.data.repository.MaintenanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Covers escalateOverdueMaintenance() end-to-end: the auto-escalation
 * feature flag guard, the one-level-per-pass priority bump
 * (LOW->MEDIUM->HIGH->CRITICAL), the CRITICAL ceiling, and the
 * EscalationEmailContent notification path added alongside it - none of
 * this had any test coverage before.
 */
@ExtendWith(MockitoExtension.class)
class MaintenanceEscalationServiceTest {

    @Mock
    private MaintenanceRepository maintenanceRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private AuditEventService auditEventService;

    private MaintenanceEscalationService service;

    @BeforeEach
    void setUp() {
        service = new MaintenanceEscalationService(maintenanceRepository, emailService, auditEventService);
        // @Value fields only get resolved inside a real Spring context - set
        // them directly here, same values as the class's own defaults.
        ReflectionTestUtils.setField(service, "daysOverdueThreshold", 7);
    }

    private Maintenance overdueMaintenance(MaintenancePriority priority) {
        Maintenance maintenance = new Maintenance();
        maintenance.setId(10L);
        maintenance.setMachineId(42L);
        maintenance.setPriority(priority);
        maintenance.setStatus(MaintenanceStatus.SCHEDULED);
        maintenance.setScheduledDate(LocalDateTime.of(2026, 8, 1, 9, 0));
        return maintenance;
    }

    @Test
    void doesNothingWhenAutoEscalationIsDisabled() {
        ReflectionTestUtils.setField(service, "autoEscalationEnabled", false);

        service.escalateOverdueMaintenance();

        verifyNoInteractions(maintenanceRepository, emailService, auditEventService);
    }

    @Test
    void escalatesOverdueLowPriorityMaintenanceByOneLevelAndNotifies() {
        ReflectionTestUtils.setField(service, "autoEscalationEnabled", true);
        Maintenance maintenance = overdueMaintenance(MaintenancePriority.LOW);
        when(maintenanceRepository.findByStatusInAndScheduledDateBefore(anyList(), any(LocalDateTime.class)))
                .thenReturn(List.of(maintenance));

        service.escalateOverdueMaintenance();

        assertEquals(MaintenancePriority.MEDIUM, maintenance.getPriority());
        verify(maintenanceRepository, times(1)).save(maintenance);
        verify(auditEventService, times(1)).record(
                eq("SYSTEM"), eq("MAINTENANCE_AUTO_ESCALATED"), eq("Maintenance"), eq(10L), any(String.class));
        verify(emailService, times(1)).sendEscalationNotification(any(EscalationEmailContent.class));
    }

    @Test
    void alreadyCriticalMaintenanceIsSkippedWithNoSaveOrNotification() {
        ReflectionTestUtils.setField(service, "autoEscalationEnabled", true);
        Maintenance maintenance = overdueMaintenance(MaintenancePriority.CRITICAL);
        when(maintenanceRepository.findByStatusInAndScheduledDateBefore(anyList(), any(LocalDateTime.class)))
                .thenReturn(List.of(maintenance));

        service.escalateOverdueMaintenance();

        assertEquals(MaintenancePriority.CRITICAL, maintenance.getPriority());
        verify(maintenanceRepository, never()).save(any(Maintenance.class));
        verifyNoInteractions(emailService);
        verifyNoInteractions(auditEventService);
    }
}
