package com.pfe.predictive.config;

import com.pfe.predictive.calendar.entity.CalendarEvent;
import com.pfe.predictive.calendar.repository.CalendarEventRepository;
import com.pfe.predictive.inventory.entity.Part;
import com.pfe.predictive.inventory.entity.PartStatus;
import com.pfe.predictive.inventory.repository.PartRepository;
import com.pfe.predictive.maintenancecost.entity.MaintenanceBudget;
import com.pfe.predictive.maintenancecost.repository.MaintenanceBudgetRepository;
import com.pfe.predictive.core.entity.Task;
import com.pfe.predictive.data.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Comprehensive Data Initializer
 * Seeds the database with complete sample data for all modules
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ComprehensiveDataInitializer implements CommandLineRunner {

    private final PartRepository partRepository;
    private final MaintenanceBudgetRepository budgetRepository;
    private final TaskRepository taskRepository;
    private final CalendarEventRepository calendarEventRepository;

    @Override
    public void run(String... args) {
        // Skip if data already exists
        if (partRepository.count() > 0) {
            log.info("✅ Database already initialized - skipping seeding");
            return;
        }

        log.info("🌱 Starting comprehensive database seeding...");

        try {
            seedParts();
            seedBudgets();
            seedTasks();
            seedCalendarEvents();
            log.info("✅ Database seeding completed successfully!");
        } catch (Exception e) {
            log.error("❌ Error during database seeding", e);
        }
    }

    /**
     * Seed parts with categories and sub-categories
     */
    private void seedParts() {
        log.info("🔧 Seeding parts...");

        List<Part> parts = new ArrayList<>();

        // Bearings
        parts.add(Part.builder()
                .partNumber("BRG-001")
                .name("Ball Bearing 6205")
                .description("Deep groove ball bearing for general machinery")
                .category("Bearings")
                .subCategory("Ball Bearings")
                .cost(new BigDecimal("45.50"))
                .currentStock(25)
                .minimumStock(5)
                .reorderQuantity(20)
                .unit("PCS")
                .supplier("SKF")
                .status(PartStatus.AVAILABLE)
                .notes("Standard bearing for CNC machines")
                .build());

        parts.add(Part.builder()
                .partNumber("BRG-002")
                .name("Roller Bearing NJ2205")
                .description("Cylindrical roller bearing")
                .category("Bearings")
                .subCategory("Roller Bearings")
                .cost(new BigDecimal("78.00"))
                .currentStock(12)
                .minimumStock(3)
                .reorderQuantity(10)
                .unit("PCS")
                .supplier("FAG")
                .status(PartStatus.AVAILABLE)
                .notes("For high-speed applications")
                .build());

        // Seals
        parts.add(Part.builder()
                .partNumber("SEAL-001")
                .name("Oil Seal 40x62x10")
                .description("Radial shaft oil seal")
                .category("Seals")
                .subCategory("Oil Seals")
                .cost(new BigDecimal("12.75"))
                .currentStock(50)
                .minimumStock(10)
                .reorderQuantity(40)
                .unit("PCS")
                .supplier("Freudenberg")
                .status(PartStatus.AVAILABLE)
                .notes("Prevents oil leakage")
                .build());

        parts.add(Part.builder()
                .partNumber("SEAL-002")
                .name("Hydraulic Seal Kit")
                .description("Complete hydraulic seal kit")
                .category("Seals")
                .subCategory("Hydraulic Seals")
                .cost(new BigDecimal("125.00"))
                .currentStock(8)
                .minimumStock(2)
                .reorderQuantity(5)
                .unit("KIT")
                .supplier("Parker")
                .status(PartStatus.AVAILABLE)
                .notes("For hydraulic cylinders")
                .build());

        // Belts
        parts.add(Part.builder()
                .partNumber("BELT-001")
                .name("V-Belt A-100")
                .description("Standard V-belt for power transmission")
                .category("Belts")
                .subCategory("V-Belts")
                .cost(new BigDecimal("35.00"))
                .currentStock(30)
                .minimumStock(5)
                .reorderQuantity(20)
                .unit("PCS")
                .supplier("Bando")
                .status(PartStatus.AVAILABLE)
                .notes("100mm length")
                .build());

        parts.add(Part.builder()
                .partNumber("BELT-002")
                .name("Timing Belt HTD-200")
                .description("Synchronous timing belt")
                .category("Belts")
                .subCategory("Timing Belts")
                .cost(new BigDecimal("85.50"))
                .currentStock(15)
                .minimumStock(3)
                .reorderQuantity(10)
                .unit("PCS")
                .supplier("Gates")
                .status(PartStatus.AVAILABLE)
                .notes("For precision machinery")
                .build());

        // Filters
        parts.add(Part.builder()
                .partNumber("FILT-001")
                .name("Oil Filter HF-123")
                .description("Hydraulic oil filter")
                .category("Filters")
                .subCategory("Oil Filters")
                .cost(new BigDecimal("28.00"))
                .currentStock(40)
                .minimumStock(10)
                .reorderQuantity(30)
                .unit("PCS")
                .supplier("Hydac")
                .status(PartStatus.AVAILABLE)
                .notes("For hydraulic systems")
                .build());

        parts.add(Part.builder()
                .partNumber("FILT-002")
                .name("Air Filter AF-456")
                .description("Compressed air filter")
                .category("Filters")
                .subCategory("Air Filters")
                .cost(new BigDecimal("18.50"))
                .currentStock(35)
                .minimumStock(8)
                .reorderQuantity(25)
                .unit("PCS")
                .supplier("SMC")
                .status(PartStatus.AVAILABLE)
                .notes("For pneumatic systems")
                .build());

        // Lubricants
        parts.add(Part.builder()
                .partNumber("LUB-001")
                .name("Synthetic Lubricant 5L")
                .description("High-performance synthetic lubricant")
                .category("Lubricants")
                .subCategory("Synthetic Oils")
                .cost(new BigDecimal("65.00"))
                .currentStock(20)
                .minimumStock(5)
                .reorderQuantity(15)
                .unit("CAN")
                .supplier("Mobil")
                .status(PartStatus.AVAILABLE)
                .notes("For all machinery types")
                .build());

        parts.add(Part.builder()
                .partNumber("LUB-002")
                .name("Grease NLGI-2 1kg")
                .description("General purpose grease")
                .category("Lubricants")
                .subCategory("Greases")
                .cost(new BigDecimal("22.00"))
                .currentStock(45)
                .minimumStock(10)
                .reorderQuantity(30)
                .unit("KG")
                .supplier("Shell")
                .status(PartStatus.AVAILABLE)
                .notes("For bearing lubrication")
                .build());

        // Fasteners
        parts.add(Part.builder()
                .partNumber("FAST-001")
                .name("Bolt M10x50 Grade 8.8")
                .description("High-strength bolt")
                .category("Fasteners")
                .subCategory("Bolts")
                .cost(new BigDecimal("2.50"))
                .currentStock(500)
                .minimumStock(100)
                .reorderQuantity(300)
                .unit("PCS")
                .supplier("DIN")
                .status(PartStatus.AVAILABLE)
                .notes("For structural assembly")
                .build());

        parts.add(Part.builder()
                .partNumber("FAST-002")
                .name("Nut M10 Stainless")
                .description("Stainless steel nut")
                .category("Fasteners")
                .subCategory("Nuts")
                .cost(new BigDecimal("1.80"))
                .currentStock(600)
                .minimumStock(150)
                .reorderQuantity(400)
                .unit("PCS")
                .supplier("DIN")
                .status(PartStatus.AVAILABLE)
                .notes("Corrosion resistant")
                .build());

        partRepository.saveAll(parts);
        log.info("✅ Seeded {} parts", parts.size());
    }

    /**
     * Seed maintenance budgets
     */
    private void seedBudgets() {
        log.info("💰 Seeding budgets...");

        List<MaintenanceBudget> budgets = new ArrayList<>();

        budgets.add(MaintenanceBudget.builder()
                .department("Production")
                .period("2026-Q2")
                .allocatedAmount(new BigDecimal("50000.00"))
                .spentAmount(new BigDecimal("15000.00"))
                .remainingAmount(new BigDecimal("35000.00"))
                .build());

        budgets.add(MaintenanceBudget.builder()
                .department("Assembly")
                .period("2026-Q2")
                .allocatedAmount(new BigDecimal("35000.00"))
                .spentAmount(new BigDecimal("8500.00"))
                .remainingAmount(new BigDecimal("26500.00"))
                .build());

        budgets.add(MaintenanceBudget.builder()
                .department("Packaging")
                .period("2026-Q2")
                .allocatedAmount(new BigDecimal("25000.00"))
                .spentAmount(new BigDecimal("12000.00"))
                .remainingAmount(new BigDecimal("13000.00"))
                .build());

        budgets.add(MaintenanceBudget.builder()
                .department("Production")
                .period("2026-Q3")
                .allocatedAmount(new BigDecimal("55000.00"))
                .spentAmount(new BigDecimal("0.00"))
                .remainingAmount(new BigDecimal("55000.00"))
                .build());

        budgetRepository.saveAll(budgets);
        log.info("✅ Seeded {} budgets", budgets.size());
    }

    /**
     * Seed tasks
     */
    private void seedTasks() {
        log.info("📋 Seeding tasks...");

        List<Task> tasks = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        tasks.add(Task.builder()
                .title("CNC Machine Maintenance")
                .description("Routine maintenance for CNC Mill Alpha")
                .status("PENDING")
                .priority("HIGH")
                .machineId(1L)
                .assignedTo("technician1")
                .dueDate(now.plusDays(2))
                .createdAt(now)
                .build());

        tasks.add(Task.builder()
                .title("Hydraulic System Check")
                .description("Check hydraulic pressure and fluid levels")
                .status("PENDING")
                .priority("MEDIUM")
                .machineId(2L)
                .assignedTo("technician2")
                .dueDate(now.plusDays(3))
                .createdAt(now)
                .build());

        tasks.add(Task.builder()
                .title("Belt Replacement")
                .description("Replace worn V-belt on conveyor system")
                .status("IN_PROGRESS")
                .priority("HIGH")
                .machineId(3L)
                .assignedTo("technician1")
                .dueDate(now.plusDays(1))
                .createdAt(now)
                .build());

        tasks.add(Task.builder()
                .title("Bearing Inspection")
                .description("Inspect and lubricate all bearings")
                .status("COMPLETED")
                .priority("MEDIUM")
                .machineId(1L)
                .assignedTo("technician3")
                .dueDate(now.minusDays(1))
                .completedDate(now)
                .createdAt(now.minusDays(2))
                .build());

        tasks.add(Task.builder()
                .title("Filter Change")
                .description("Replace oil and air filters")
                .status("PENDING")
                .priority("LOW")
                .machineId(2L)
                .assignedTo("technician2")
                .dueDate(now.plusDays(5))
                .createdAt(now)
                .build());

        taskRepository.saveAll(tasks);
        log.info("✅ Seeded {} tasks", tasks.size());
    }

    /**
     * Seed calendar events
     */
    private void seedCalendarEvents() {
        log.info("📅 Seeding calendar events...");

        List<CalendarEvent> events = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // Today's events
        events.add(CalendarEvent.builder()
                .title("Morning Maintenance Meeting")
                .description("Daily standup for maintenance team")
                .startTime(now.withHour(9).withMinute(0))
                .endTime(now.withHour(9).withMinute(30))
                .eventType("MEETING")
                .assignedTo("technician1")
                .status("SCHEDULED")
                .priority("MEDIUM")
                .location("Conference Room A")
                .isAllDay(false)
                .isRecurring(true)
                .recurrencePattern("DAILY")
                .createdBy("admin")
                .build());

        // Tomorrow's events
        events.add(CalendarEvent.builder()
                .title("CNC Machine Inspection")
                .description("Detailed inspection of CNC Mill Alpha")
                .startTime(now.plusDays(1).withHour(10).withMinute(0))
                .endTime(now.plusDays(1).withHour(12).withMinute(0))
                .eventType("MAINTENANCE")
                .assignedTo("technician1")
                .status("SCHEDULED")
                .priority("HIGH")
                .machineId(1L)
                .isAllDay(false)
                .createdBy("admin")
                .build());

        events.add(CalendarEvent.builder()
                .title("Hydraulic System Maintenance")
                .description("Check and service hydraulic system")
                .startTime(now.plusDays(1).withHour(14).withMinute(0))
                .endTime(now.plusDays(1).withHour(16).withMinute(0))
                .eventType("MAINTENANCE")
                .assignedTo("technician2")
                .status("SCHEDULED")
                .priority("HIGH")
                .machineId(2L)
                .isAllDay(false)
                .createdBy("admin")
                .build());

        // Next week events
        events.add(CalendarEvent.builder()
                .title("Equipment Calibration")
                .description("Calibrate all measurement equipment")
                .startTime(now.plusDays(7).withHour(9).withMinute(0))
                .endTime(now.plusDays(7).withHour(11).withMinute(0))
                .eventType("INSPECTION")
                .assignedTo("technician3")
                .status("SCHEDULED")
                .priority("MEDIUM")
                .isAllDay(false)
                .createdBy("admin")
                .build());

        events.add(CalendarEvent.builder()
                .title("Preventive Maintenance - All Machines")
                .description("Monthly preventive maintenance for all equipment")
                .startTime(now.plusDays(14).withHour(8).withMinute(0))
                .endTime(now.plusDays(14).withHour(17).withMinute(0))
                .eventType("MAINTENANCE")
                .assignedTo("technician1")
                .status("SCHEDULED")
                .priority("HIGH")
                .isAllDay(true)
                .isRecurring(true)
                .recurrencePattern("MONTHLY")
                .createdBy("admin")
                .build());

        events.add(CalendarEvent.builder()
                .title("Team Training - Safety Procedures")
                .description("Quarterly safety training for maintenance team")
                .startTime(now.plusDays(21).withHour(10).withMinute(0))
                .endTime(now.plusDays(21).withHour(12).withMinute(0))
                .eventType("MEETING")
                .assignedTo("technician2")
                .status("SCHEDULED")
                .priority("MEDIUM")
                .location("Training Room")
                .isAllDay(false)
                .createdBy("admin")
                .build());

        calendarEventRepository.saveAll(events);
        log.info("✅ Seeded {} calendar events", events.size());
    }
}
