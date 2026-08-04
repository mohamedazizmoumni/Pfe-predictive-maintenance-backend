package com.pfe.predictive.ml.service;

import com.pfe.predictive.core.entity.Machine;
import com.pfe.predictive.core.entity.SensorTelemetry;
import com.pfe.predictive.data.repository.MachineRepository;
import com.pfe.predictive.data.repository.SensorTelemetryRepository;
import com.pfe.predictive.ml.dto.MLFeatures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * Covers FeatureEngineeringService's statistical computations (average,
 * max, min, std-dev, linear-regression trend, and trend-of-trend
 * acceleration) with hand-computed expected values, plus the guard clauses
 * around missing telemetry/machine data.
 */
@ExtendWith(MockitoExtension.class)
class FeatureEngineeringServiceTest {

    private static final double DELTA = 1e-6;

    @Mock
    private SensorTelemetryRepository telemetryRepository;

    @Mock
    private MachineRepository machineRepository;

    private FeatureEngineeringService service;

    @BeforeEach
    void setUp() {
        service = new FeatureEngineeringService(telemetryRepository, machineRepository);
    }

    private SensorTelemetry reading(double sensor1) {
        SensorTelemetry t = new SensorTelemetry();
        t.setSensor1(sensor1);
        t.setSensor2(0.0);
        t.setSensor3(0.0);
        t.setSensor4(0.0);
        return t;
    }

    private Machine machine(Double operatingHours, LocalDateTime lastMaintenanceDate) {
        Machine m = new Machine();
        m.setOperatingHours(operatingHours);
        m.setLastMaintenanceDate(lastMaintenanceDate);
        return m;
    }

    @Test
    void throwsWhenNoTelemetryInWindow() {
        when(telemetryRepository.findByMachineIdAndTimestampBetween(anyLong(), any(), any()))
                .thenReturn(List.of());

        assertThrows(IllegalStateException.class, () -> service.engineerFeatures(1L));
    }

    @Test
    void throwsWhenMachineNotFound() {
        when(telemetryRepository.findByMachineIdAndTimestampBetween(anyLong(), any(), any()))
                .thenReturn(List.of(reading(10.0)));
        when(machineRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.engineerFeatures(1L));
    }

    @Test
    void computesAverageMaxMinStdDevTrendAndAcceleration() {
        // Hand-computed against sensor1 = [10, 12, 14, 20]:
        //   avg = 14.0, max = 20.0, min = 10.0
        //   variance = ((10-14)^2+(12-14)^2+(14-14)^2+(20-14)^2)/4 = 14.0 -> stddev = sqrt(14)
        //   full-set trend (linear regression slope over index 0..3, scaled to per-hour
        //   over a 24h/4-point window) = 3.2 / 6 = 0.533333...
        //   acceleration: split at (int)(4*0.75)=3 -> historical=[10,12,14] (trend 0.25 per hour),
        //   recent=[20] (single point -> trend 0.0) -> acceleration = 0 - 0.25 = -0.25
        List<SensorTelemetry> telemetry = List.of(reading(10.0), reading(12.0), reading(14.0), reading(20.0));
        when(telemetryRepository.findByMachineIdAndTimestampBetween(anyLong(), any(), any()))
                .thenReturn(telemetry);
        when(machineRepository.findById(1L)).thenReturn(Optional.of(machine(500.0, null)));

        MLFeatures features = service.engineerFeatures(1L);

        assertEquals(14.0, features.getTemperatureAvg(), DELTA);
        assertEquals(20.0, features.getTemperatureMax(), DELTA);
        assertEquals(10.0, features.getTemperatureMin(), DELTA);
        assertEquals(Math.sqrt(14.0), features.getTemperatureStdDev(), DELTA);
        assertEquals(0.533333333333, features.getTemperatureTrend(), 1e-9);
        assertEquals(-0.25, features.getTemperatureAcceleration(), 1e-9);
    }

    @Test
    void defaultsOperatingHoursToZeroAndMaintenanceGapTo365DaysWhenMissing() {
        when(telemetryRepository.findByMachineIdAndTimestampBetween(anyLong(), any(), any()))
                .thenReturn(List.of(reading(10.0)));
        when(machineRepository.findById(1L)).thenReturn(Optional.of(machine(null, null)));

        MLFeatures features = service.engineerFeatures(1L);

        assertEquals(0L, features.getOperatingHours());
        assertEquals(365L, features.getDaysSinceLastMaintenance());
    }

    @Test
    void computesDaysSinceLastMaintenanceFromMachineRecord() {
        when(telemetryRepository.findByMachineIdAndTimestampBetween(anyLong(), any(), any()))
                .thenReturn(List.of(reading(10.0)));
        when(machineRepository.findById(1L))
                .thenReturn(Optional.of(machine(200.0, LocalDateTime.now().minusDays(10))));

        MLFeatures features = service.engineerFeatures(1L);

        assertEquals(10L, features.getDaysSinceLastMaintenance());
        assertEquals(200L, features.getOperatingHours());
    }
}
