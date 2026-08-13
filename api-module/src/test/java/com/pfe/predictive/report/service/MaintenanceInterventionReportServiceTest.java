package com.pfe.predictive.report.service;

import com.pfe.predictive.core.entity.Machine;
import com.pfe.predictive.core.entity.Maintenance;
import com.pfe.predictive.core.entity.MaintenancePriority;
import com.pfe.predictive.core.entity.MaintenanceRecommendation;
import com.pfe.predictive.core.entity.MaintenanceStatus;
import com.pfe.predictive.core.entity.MaintenanceType;
import com.pfe.predictive.core.entity.finance.ChecklistItem;
import com.pfe.predictive.core.entity.finance.MaintenanceRapport;
import com.pfe.predictive.core.entity.finance.RapportPart;
import com.pfe.predictive.core.entity.finance.RapportStatus;
import com.pfe.predictive.data.repository.MachineRepository;
import com.pfe.predictive.data.repository.MaintenanceRecommendationRepository;
import com.pfe.predictive.data.repository.MaintenanceRepository;
import com.pfe.predictive.data.repository.finance.MaintenanceRapportRepository;
import com.pfe.predictive.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * Real PDF generation, not just a compile check — OpenPDF's failure modes
 * (bad font metrics, mismatched table column counts, unclosed documents)
 * only show up at runtime. Asserts the actual byte output is a well-formed
 * PDF (starts with the %PDF magic header) for both a minimal rapport and
 * one exercising every optional section (checklist, parts, AI recommendation).
 */
@ExtendWith(MockitoExtension.class)
class MaintenanceInterventionReportServiceTest {

    @Mock private MaintenanceRapportRepository rapportRepository;
    @Mock private MachineRepository machineRepository;
    @Mock private MaintenanceRepository maintenanceRepository;
    @Mock private MaintenanceRecommendationRepository recommendationRepository;

    private MaintenanceInterventionReportService service;

    private MaintenanceRapport minimalRapport() {
        return MaintenanceRapport.builder()
                .id(1L)
                .machineId(10L)
                .machineName("Press-14")
                .technicianName("Jane Tech")
                .workPerformed("Replaced worn bearing.")
                .laborHours(2.5)
                .totalCost(BigDecimal.valueOf(150.75))
                .status(RapportStatus.PENDING_MANAGER_APPROVAL)
                .createdDate(LocalDateTime.now())
                .parts(List.of())
                .checklistItems(List.of())
                .build();
    }

    @Test
    void generatesValidPdfForMinimalRapportWithNoOptionalSections() {
        service = new MaintenanceInterventionReportService(rapportRepository, machineRepository, maintenanceRepository, recommendationRepository);
        MaintenanceRapport rapport = minimalRapport();

        when(rapportRepository.findById(1L)).thenReturn(Optional.of(rapport));
        when(machineRepository.findById(10L)).thenReturn(Optional.empty());

        byte[] pdf = service.generate(1L);

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
    }

    @Test
    void generatesValidPdfWithChecklistPartsMachineAndRecommendation() {
        service = new MaintenanceInterventionReportService(rapportRepository, machineRepository, maintenanceRepository, recommendationRepository);

        MaintenanceRapport rapport = minimalRapport();
        rapport.setTaskId(20L);
        rapport.setApprovedByManager("mgr1");
        rapport.setManagerApprovedDate(LocalDateTime.now());
        rapport.setChecklistItems(List.of(
                ChecklistItem.builder().description("Bearing lubrication verified").passed(true).build(),
                ChecklistItem.builder().description("Belt tension checked").passed(false).notes("Needs replacement next cycle").build()
        ));
        rapport.setParts(List.of(
                RapportPart.builder().partId(5L).partName("Motor Bearing").quantity(1)
                        .unitCost(BigDecimal.valueOf(45.0)).totalCost(BigDecimal.valueOf(45.0)).build()
        ));

        Machine machine = new Machine();
        machine.setId(10L);
        machine.setName("Press-14");
        machine.setSerialNumber("SN-14");
        machine.setStatus("OPERATIONAL");

        Maintenance maintenance = new Maintenance();
        maintenance.setId(20L);
        maintenance.setType(MaintenanceType.CORRECTIVE);
        maintenance.setStatus(MaintenanceStatus.COMPLETED);
        maintenance.setPriority(MaintenancePriority.HIGH);
        maintenance.setDescription("Unusual vibration reported by technician.");

        MaintenanceRecommendation recommendation = MaintenanceRecommendation.builder()
                .id(99L)
                .urgencyLevel("HIGH")
                .recommendedAction("PREVENTIVE")
                .justification("Health dropped below threshold.")
                .decidedBy("mgr1")
                .resultingMaintenanceId(20L)
                .build();

        when(rapportRepository.findById(1L)).thenReturn(Optional.of(rapport));
        when(machineRepository.findById(10L)).thenReturn(Optional.of(machine));
        when(maintenanceRepository.findById(20L)).thenReturn(Optional.of(maintenance));
        when(recommendationRepository.findByResultingMaintenanceId(20L)).thenReturn(Optional.of(recommendation));

        byte[] pdf = service.generate(1L);

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
        // A report with two extra sections (checklist + parts + AI block)
        // should meaningfully outsize the minimal one - a crude but real
        // signal the extra content actually got rendered, not silently dropped.
        assertThat(pdf.length).isGreaterThan(1500);
    }

    @Test
    void throwsResourceNotFoundForUnknownRapport() {
        service = new MaintenanceInterventionReportService(rapportRepository, machineRepository, maintenanceRepository, recommendationRepository);
        when(rapportRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.generate(404L));
    }
}
