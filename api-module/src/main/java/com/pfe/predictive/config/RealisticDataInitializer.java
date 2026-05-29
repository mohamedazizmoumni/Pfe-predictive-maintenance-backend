package com.pfe.predictive.config;

import com.pfe.predictive.core.entity.*;
import com.pfe.predictive.data.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Realistic Data Initializer
 * Seeds the database with comprehensive realistic data for testing and demonstration
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RealisticDataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final MachineRepository machineRepository;
    private final SensorRepository sensorRepository;
    private final MachineSimulationStateRepository machineSimulationStateRepository;
    private final SensorTelemetryRepository sensorTelemetryRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public void run(String... args) {
        // Skip if machines already exist (indicates data is already seeded)
        if (machineRepository.count() > 0) {
            log.info("✅ Realistic data already initialized - skipping");
            return;
        }

        log.info("🌱 Starting realistic database seeding...");

        try {
            seedUsers();
            seedMachines();
            seedSensors();
            seedMachineSimulationStates();
            seedSensorTelemetry();
            log.info("✅ Realistic database seeding completed successfully!");
        } catch (Exception e) {
            log.error("❌ Error during realistic database seeding", e);
        }
    }

    /**
     * Seed realistic users with different roles and departments
     */
    private void seedUsers() {
        log.info("👥 Seeding realistic users...");

        // Super Admin
        createUser(
                "admin",
                "admin@predictive-maintenance.com",
                "Admin@123",
                "John",
                "Administrator",
                "System Administrator",
                "IT",
                List.of("SUPER_ADMIN", "ADMIN")
        );

        // Managers
        createUser(
                "manager_production",
                "manager.production@predictive-maintenance.com",
                "Manager@123",
                "Sarah",
                "Johnson",
                "Production Manager",
                "Production",
                List.of("MANAGER")
        );

        createUser(
                "manager_maintenance",
                "manager.maintenance@predictive-maintenance.com",
                "Manager@123",
                "Michael",
                "Chen",
                "Maintenance Manager",
                "Maintenance",
                List.of("MANAGER")
        );

        // Technicians
        createUser(
                "technician_senior",
                "technician.senior@predictive-maintenance.com",
                "Tech@123",
                "Robert",
                "Williams",
                "Senior Technician",
                "Maintenance",
                List.of("TECHNICIAN")
        );

        createUser(
                "technician_junior1",
                "technician.junior1@predictive-maintenance.com",
                "Tech@123",
                "Emily",
                "Davis",
                "Junior Technician",
                "Maintenance",
                List.of("TECHNICIAN")
        );

        createUser(
                "technician_junior2",
                "technician.junior2@predictive-maintenance.com",
                "Tech@123",
                "David",
                "Martinez",
                "Junior Technician",
                "Maintenance",
                List.of("TECHNICIAN")
        );

        // Data Scientists
        createUser(
                "data_scientist",
                "data.scientist@predictive-maintenance.com",
                "Data@123",
                "Lisa",
                "Anderson",
                "Data Scientist",
                "Analytics",
                List.of("DATA_SCIENTIST")
        );

        // Viewers
        createUser(
                "viewer",
                "viewer@predictive-maintenance.com",
                "View@123",
                "James",
                "Thompson",
                "Operations Viewer",
                "Operations",
                List.of("VIEWER")
        );

        log.info("✅ Seeded 8 users");
    }

    /**
     * Seed realistic machines with different categories
     */
    private void seedMachines() {
        log.info("🏭 Seeding realistic machines...");

        List<Machine> machines = new ArrayList<>();

        // CNC Machines
        machines.add(createMachine(
                "CNC-MILL-001",
                "CNC Milling Machine Alpha",
                "VMC-1050",
                "Haas",
                "Production Floor - Zone A",
                "CNC Machines",
                "Vertical Mills",
                "OPERATIONAL"
        ));

        machines.add(createMachine(
                "CNC-MILL-002",
                "CNC Milling Machine Beta",
                "VMC-1050",
                "Haas",
                "Production Floor - Zone A",
                "CNC Machines",
                "Vertical Mills",
                "OPERATIONAL"
        ));

        machines.add(createMachine(
                "CNC-LATHE-001",
                "CNC Lathe Gamma",
                "ST-40",
                "Okuma",
                "Production Floor - Zone B",
                "CNC Machines",
                "Lathes",
                "OPERATIONAL"
        ));

        // Hydraulic Machines
        machines.add(createMachine(
                "HYD-PRESS-001",
                "Hydraulic Press Delta",
                "HP-500",
                "Enerpac",
                "Assembly Floor - Zone C",
                "Hydraulic Equipment",
                "Presses",
                "OPERATIONAL"
        ));

        machines.add(createMachine(
                "HYD-PUMP-001",
                "Hydraulic Pump Station",
                "PVH-131",
                "Eaton",
                "Utility Room",
                "Hydraulic Equipment",
                "Pumps",
                "OPERATIONAL"
        ));

        // Conveyor Systems
        machines.add(createMachine(
                "CONV-001",
                "Main Conveyor System",
                "CV-2000",
                "Siemens",
                "Production Floor - Zone D",
                "Conveyor Systems",
                "Belt Conveyors",
                "OPERATIONAL"
        ));

        machines.add(createMachine(
                "CONV-002",
                "Secondary Conveyor",
                "CV-1500",
                "Siemens",
                "Packaging Area",
                "Conveyor Systems",
                "Belt Conveyors",
                "OPERATIONAL"
        ));

        // Compressors
        machines.add(createMachine(
                "COMP-001",
                "Air Compressor Unit",
                "AC-75",
                "Atlas Copco",
                "Utility Room",
                "Compressors",
                "Rotary Screw",
                "OPERATIONAL"
        ));

        // Packaging Equipment
        machines.add(createMachine(
                "PKG-001",
                "Automatic Packaging Machine",
                "APM-500",
                "Bosch",
                "Packaging Area",
                "Packaging Equipment",
                "Automatic Packers",
                "OPERATIONAL"
        ));

        // Welding Equipment
        machines.add(createMachine(
                "WELD-001",
                "Robotic Welding Station",
                "IRB-6700",
                "ABB",
                "Assembly Floor - Zone E",
                "Welding Equipment",
                "Robotic Welders",
                "MAINTENANCE"
        ));

        machineRepository.saveAll(machines);
        log.info("✅ Seeded {} machines", machines.size());
    }

    /**
     * Seed sensors for each machine
     */
    private void seedSensors() {
        log.info("📊 Seeding sensors...");

        List<Sensor> sensors = new ArrayList<>();
        List<Machine> machines = machineRepository.findAll();

        for (Machine machine : machines) {
            // Temperature sensor
            Sensor tempSensor = new Sensor();
            tempSensor.setCode(machine.getSerialNumber() + "-TEMP");
            tempSensor.setName("Temperature Sensor");
            tempSensor.setType("TEMPERATURE");
            tempSensor.setUnit("°C");
            tempSensor.setMinRange(20.0);
            tempSensor.setMaxRange(80.0);
            tempSensor.setMachine(machine);
            sensors.add(tempSensor);

            // Vibration sensor
            Sensor vibSensor = new Sensor();
            vibSensor.setCode(machine.getSerialNumber() + "-VIB");
            vibSensor.setName("Vibration Sensor");
            vibSensor.setType("VIBRATION");
            vibSensor.setUnit("mm/s");
            vibSensor.setMinRange(0.0);
            vibSensor.setMaxRange(10.0);
            vibSensor.setMachine(machine);
            sensors.add(vibSensor);

            // Pressure sensor
            Sensor pressSensor = new Sensor();
            pressSensor.setCode(machine.getSerialNumber() + "-PRESS");
            pressSensor.setName("Pressure Sensor");
            pressSensor.setType("PRESSURE");
            pressSensor.setUnit("PSI");
            pressSensor.setMinRange(0.0);
            pressSensor.setMaxRange(150.0);
            pressSensor.setMachine(machine);
            sensors.add(pressSensor);

            // Power consumption sensor
            Sensor powerSensor = new Sensor();
            powerSensor.setCode(machine.getSerialNumber() + "-POWER");
            powerSensor.setName("Power Sensor");
            powerSensor.setType("POWER");
            powerSensor.setUnit("kW");
            powerSensor.setMinRange(0.0);
            powerSensor.setMaxRange(100.0);
            powerSensor.setMachine(machine);
            sensors.add(powerSensor);

            // Humidity sensor (for certain machines)
            if (machine.getCategory() != null && 
                (machine.getCategory().equals("Hydraulic Equipment") || 
                 machine.getCategory().equals("Compressors"))) {
                Sensor humiditySensor = new Sensor();
                humiditySensor.setCode(machine.getSerialNumber() + "-HUM");
                humiditySensor.setName("Humidity Sensor");
                humiditySensor.setType("HUMIDITY");
                humiditySensor.setUnit("%");
                humiditySensor.setMinRange(20.0);
                humiditySensor.setMaxRange(80.0);
                humiditySensor.setMachine(machine);
                sensors.add(humiditySensor);
            }
        }

        sensorRepository.saveAll(sensors);
        log.info("✅ Seeded {} sensors", sensors.size());
    }

    /**
     * Seed machine simulation states
     */
    private void seedMachineSimulationStates() {
        log.info("⚙️ Seeding machine simulation states...");

        List<Machine> machines = machineRepository.findAll();
        List<MachineSimulationState> states = new ArrayList<>();

        for (Machine machine : machines) {
            // Healthy machines
            if (machine.getStatus().equals("OPERATIONAL") && 
                !machine.getSerialNumber().contains("WELD")) {
                states.add(MachineSimulationState.initializeHealthy(machine.getId()));
            }
            // Machine under maintenance
            else if (machine.getStatus().equals("MAINTENANCE")) {
                states.add(MachineSimulationState.initializePartiallyDegraded(machine.getId()));
            }
            // Default healthy state
            else {
                states.add(MachineSimulationState.initializeHealthy(machine.getId()));
            }
        }

        machineSimulationStateRepository.saveAll(states);
        log.info("✅ Seeded {} machine simulation states", states.size());
    }

    /**
     * Seed sensor telemetry data
     */
    private void seedSensorTelemetry() {
        log.info("📈 Seeding sensor telemetry data...");

        List<Sensor> sensors = sensorRepository.findAll();
        List<SensorTelemetry> telemetryList = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        Random random = new Random();

        // Generate telemetry data for the last 24 hours
        for (Sensor sensor : sensors) {
            for (int i = 0; i < 24; i++) {
                LocalDateTime timestamp = now.minusHours(24 - i);
                
                SensorTelemetry telemetry = new SensorTelemetry();
                telemetry.setMachineId(sensor.getMachine().getId());
                telemetry.setTimestamp(timestamp);
                telemetry.setCreatedAt(timestamp);

                // Generate realistic sensor values based on sensor type
                String sensorType = sensor.getType();
                if (sensorType != null) {
                    switch (sensorType) {
                        case "TEMPERATURE":
                            telemetry.setSensor1(45.0 + random.nextDouble() * 20); // Temperature
                            break;
                        case "VIBRATION":
                            telemetry.setSensor2(2.0 + random.nextDouble() * 5); // Vibration
                            break;
                        case "PRESSURE":
                            telemetry.setSensor3(80.0 + random.nextDouble() * 40); // Pressure
                            break;
                        case "POWER":
                            telemetry.setSensor4(30.0 + random.nextDouble() * 50); // Power
                            break;
                        case "HUMIDITY":
                            telemetry.setSensor5(40.0 + random.nextDouble() * 30); // Humidity
                            break;
                    }
                }

                // Add operational settings
                telemetry.setSetting1(70.0); // Ambient temperature
                telemetry.setSetting2(0.6); // Load factor
                telemetry.setSetting3(60.0); // Operating speed

                telemetryList.add(telemetry);
            }
        }

        sensorTelemetryRepository.saveAll(telemetryList);
        log.info("✅ Seeded {} sensor telemetry records", telemetryList.size());
    }

    /**
     * Helper method to create a user
     */
    private void createUser(String username, String email, String password, 
                           String firstName, String lastName, String displayName,
                           String department, List<String> roleNames) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setDisplayName(displayName);
        user.setDepartment(department);
        user.setStatus("ACTIVE");
        user.setLocked(false);
        user.setMfaEnabled(false);

        for (String roleName : roleNames) {
            roleRepository.findByName(roleName).ifPresent(user::addRole);
        }

        userRepository.save(user);
        log.info("✅ Created user: {} ({})", username, displayName);
    }

    /**
     * Helper method to create a machine
     */
    private Machine createMachine(String serialNumber, String name, String model,
                                 String manufacturer, String location, String category,
                                 String subCategory, String status) {
        Machine machine = new Machine();
        machine.setSerialNumber(serialNumber);
        machine.setName(name);
        machine.setModel(model);
        machine.setManufacturer(manufacturer);
        machine.setLocation(location);
        machine.setCategory(category);
        machine.setSubCategory(subCategory);
        machine.setStatus(status);
        machine.setInstallationDate(LocalDateTime.now().minusMonths(6));
        machine.setLastMaintenanceDate(LocalDateTime.now().minusDays(30));
        machine.setNextMaintenanceDate(LocalDateTime.now().plusDays(30));
        machine.setOperatingHours(5000.0);
        machine.setRiskScore(0.3);
        return machine;
    }
}
