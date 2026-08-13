package com.pfe.predictive.report.service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.pfe.predictive.core.entity.Machine;
import com.pfe.predictive.core.entity.Maintenance;
import com.pfe.predictive.core.entity.MaintenanceRecommendation;
import com.pfe.predictive.core.entity.finance.ChecklistItem;
import com.pfe.predictive.core.entity.finance.MaintenanceRapport;
import com.pfe.predictive.core.entity.finance.RapportPart;
import com.pfe.predictive.data.repository.MachineRepository;
import com.pfe.predictive.data.repository.MaintenanceRecommendationRepository;
import com.pfe.predictive.data.repository.MaintenanceRepository;
import com.pfe.predictive.data.repository.finance.MaintenanceRapportRepository;
import com.pfe.predictive.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Priority 5: the first server-side PDF report. Deliberately started with
 * just the Maintenance Intervention Report per the instruction to not build
 * all three report types before the first one is proven out — Machine
 * Health and Fleet/Management reports are documented as future work, not
 * built here.
 *
 * <p>Every field printed is a real value from MaintenanceRapport (and the
 * Maintenance/Machine/recommendation rows it references) — nothing here
 * computes new business data, this is presentation only.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MaintenanceInterventionReportService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a");

    private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, new Color(15, 23, 42));
    private static final Font SECTION_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, new Color(37, 99, 235));
    private static final Font LABEL_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, new Color(100, 116, 139));
    private static final Font VALUE_FONT = FontFactory.getFont(FontFactory.HELVETICA, 10, new Color(15, 23, 42));
    private static final Font SMALL_FONT = FontFactory.getFont(FontFactory.HELVETICA, 8, new Color(148, 163, 184));

    private final MaintenanceRapportRepository rapportRepository;
    private final MachineRepository machineRepository;
    private final MaintenanceRepository maintenanceRepository;
    private final MaintenanceRecommendationRepository recommendationRepository;

    public byte[] generate(Long rapportId) {
        MaintenanceRapport rapport = rapportRepository.findById(rapportId)
                .orElseThrow(() -> new ResourceNotFoundException("Maintenance rapport not found with id " + rapportId));

        Machine machine = rapport.getMachineId() != null
                ? machineRepository.findById(rapport.getMachineId()).orElse(null)
                : null;

        // rapport.getTaskId() is a Maintenance id in the technician-completion
        // flow that produces these rapports (same convention documented in
        // MaintenanceRapportService.recordPartUsage()).
        Maintenance maintenance = rapport.getTaskId() != null
                ? maintenanceRepository.findById(rapport.getTaskId()).orElse(null)
                : null;

        MaintenanceRecommendation triggeringRecommendation = maintenance != null
                ? findTriggeringRecommendation(maintenance.getId())
                : null;

        return render(rapport, machine, maintenance, triggeringRecommendation);
    }

    /** The approved recommendation that created this maintenance work order, if any — a manual work order has none. */
    private MaintenanceRecommendation findTriggeringRecommendation(Long maintenanceId) {
        return recommendationRepository.findByResultingMaintenanceId(maintenanceId).orElse(null);
    }

    private byte[] render(MaintenanceRapport rapport, Machine machine, Maintenance maintenance,
                           MaintenanceRecommendation recommendation) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document doc = new Document(PageSize.A4, 40, 40, 50, 50);
            PdfWriter.getInstance(doc, out);
            doc.open();

            doc.add(title("Maintenance Intervention Report"));
            doc.add(subtitle("Rapport #" + rapport.getId() + " — generated " + java.time.LocalDateTime.now().format(DATE_FORMAT)));
            doc.add(spacer());

            doc.add(sectionHeader("Overview"));
            doc.add(keyValueTable(List.of(
                    row("Machine", machine != null ? machine.getName() + " (" + machine.getSerialNumber() + ")" : rapport.getMachineName()),
                    row("Date", rapport.getCreatedDate() != null ? rapport.getCreatedDate().format(DATE_FORMAT) : "—"),
                    row("Technician", rapport.getTechnicianName()),
                    row("Maintenance Type", maintenance != null && maintenance.getType() != null ? maintenance.getType().name() : "—"),
                    row("Problem / Trigger", maintenance != null ? nullToDash(maintenance.getDescription()) : "—"),
                    row("Intervention Duration", rapport.getLaborHours() != null ? rapport.getLaborHours() + " hour(s)" : "—")
            )));
            doc.add(spacer());

            if (recommendation != null) {
                doc.add(sectionHeader("AI Recommendation"));
                doc.add(keyValueTable(List.of(
                        row("Urgency", recommendation.getUrgencyLevel()),
                        row("Recommended Action", recommendation.getRecommendedAction()),
                        row("Justification", nullToDash(recommendation.getJustification())),
                        row("Approved By", nullToDash(recommendation.getDecidedBy()))
                )));
                doc.add(spacer());
            }

            doc.add(sectionHeader("Diagnosis & Actions Performed"));
            doc.add(bodyText(nullToDash(rapport.getWorkPerformed())));
            doc.add(spacer());

            if (!rapport.getChecklistItems().isEmpty()) {
                doc.add(sectionHeader("Inspection Checklist"));
                doc.add(checklistTable(rapport.getChecklistItems()));
                doc.add(spacer());
            }

            if (!rapport.getParts().isEmpty()) {
                doc.add(sectionHeader("Parts Consumed"));
                doc.add(partsTable(rapport.getParts()));
                doc.add(spacer());
            }

            doc.add(sectionHeader("Validation"));
            doc.add(keyValueTable(List.of(
                    row("Status", rapport.getStatus() != null ? rapport.getStatus().name() : "—"),
                    row("Manager", nullToDash(rapport.getApprovedByManager())),
                    row("Manager Approved", rapport.getManagerApprovedDate() != null ? rapport.getManagerApprovedDate().format(DATE_FORMAT) : "—"),
                    row("Total Cost", rapport.getTotalCost() != null ? "€" + rapport.getTotalCost().setScale(2, java.math.RoundingMode.HALF_UP) : "—"),
                    row("Final Machine Status", machine != null ? nullToDash(machine.getStatus()) : "—")
            )));

            doc.add(spacer());
            doc.add(footer());

            doc.close();
            return out.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to generate maintenance intervention report: " + ex.getMessage(), ex);
        }
    }

    // ==================== LAYOUT HELPERS ====================

    private Paragraph title(String text) {
        Paragraph p = new Paragraph(text, TITLE_FONT);
        p.setSpacingAfter(4f);
        return p;
    }

    private Paragraph subtitle(String text) {
        Paragraph p = new Paragraph(text, SMALL_FONT);
        p.setSpacingAfter(14f);
        return p;
    }

    private Paragraph sectionHeader(String text) {
        Paragraph p = new Paragraph(text, SECTION_FONT);
        p.setSpacingBefore(4f);
        p.setSpacingAfter(6f);
        return p;
    }

    private Paragraph bodyText(String text) {
        return new Paragraph(text, VALUE_FONT);
    }

    private Paragraph spacer() {
        Paragraph p = new Paragraph(" ");
        p.setSpacingAfter(4f);
        return p;
    }

    private Paragraph footer() {
        Paragraph p = new Paragraph("This is an automatically generated document — Sentinel Predictive Maintenance Platform.", SMALL_FONT);
        p.setAlignment(Element.ALIGN_CENTER);
        return p;
    }

    private String[] row(String label, String value) {
        return new String[]{label, value == null || value.isBlank() ? "—" : value};
    }

    private PdfPTable keyValueTable(List<String[]> rows) {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        try {
            table.setWidths(new float[]{1f, 2f});
        } catch (Exception ignored) {
            // widths length always matches column count here; defensive only
        }
        for (String[] r : rows) {
            table.addCell(labelCell(r[0]));
            table.addCell(valueCell(r[1]));
        }
        return table;
    }

    private PdfPTable checklistTable(List<ChecklistItem> items) {
        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        addHeaderRow(table, "Item", "Result", "Notes");
        for (ChecklistItem item : items) {
            table.addCell(valueCell(item.getDescription()));
            table.addCell(valueCell(item.isPassed() ? "Passed" : "Failed"));
            table.addCell(valueCell(nullToDash(item.getNotes())));
        }
        return table;
    }

    private PdfPTable partsTable(List<RapportPart> parts) {
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        addHeaderRow(table, "Part", "Qty", "Unit Cost", "Total");
        for (RapportPart part : parts) {
            table.addCell(valueCell(part.getPartName()));
            table.addCell(valueCell(String.valueOf(part.getQuantity())));
            table.addCell(valueCell(part.getUnitCost() != null ? "€" + part.getUnitCost() : "—"));
            table.addCell(valueCell(part.getTotalCost() != null ? "€" + part.getTotalCost() : "—"));
        }
        return table;
    }

    private void addHeaderRow(PdfPTable table, String... headers) {
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Paragraph(h, LABEL_FONT));
            cell.setBackgroundColor(new Color(248, 250, 252));
            cell.setPadding(6f);
            cell.setBorderColor(new Color(226, 232, 240));
            table.addCell(cell);
        }
    }

    private PdfPCell labelCell(String text) {
        PdfPCell cell = new PdfPCell(new Paragraph(text, LABEL_FONT));
        cell.setPadding(5f);
        cell.setBorderColor(new Color(226, 232, 240));
        cell.setBackgroundColor(new Color(248, 250, 252));
        return cell;
    }

    private PdfPCell valueCell(String text) {
        PdfPCell cell = new PdfPCell(new Paragraph(text, VALUE_FONT));
        cell.setPadding(5f);
        cell.setBorderColor(new Color(226, 232, 240));
        return cell;
    }

    private String nullToDash(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }
}
