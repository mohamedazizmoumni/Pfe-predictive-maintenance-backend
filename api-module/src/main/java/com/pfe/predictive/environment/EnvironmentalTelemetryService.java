package com.pfe.predictive.environment;

import com.pfe.predictive.core.entity.EnvironmentalTelemetry;
import com.pfe.predictive.core.entity.Machine;
import com.pfe.predictive.data.repository.EnvironmentalTelemetryRepository;
import com.pfe.predictive.data.repository.MachineRepository;
import com.pfe.predictive.environment.dto.EnvironmentAnalysisResponse;
import com.pfe.predictive.environment.dto.EnvironmentalTelemetryDto;
import com.pfe.predictive.environment.dto.EnvironmentalTelemetryRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
public class EnvironmentalTelemetryService {

    private final MachineRepository machineRepository;
    private final EnvironmentalTelemetryRepository environmentalTelemetryRepository;
    private final EnvironmentAnalysisClient environmentAnalysisClient;
    private final SimpMessagingTemplate messagingTemplate;

    public EnvironmentalTelemetryService(
            MachineRepository machineRepository,
            EnvironmentalTelemetryRepository environmentalTelemetryRepository,
            EnvironmentAnalysisClient environmentAnalysisClient,
            SimpMessagingTemplate messagingTemplate) {
        this.machineRepository = machineRepository;
        this.environmentalTelemetryRepository = environmentalTelemetryRepository;
        this.environmentAnalysisClient = environmentAnalysisClient;
        this.messagingTemplate = messagingTemplate;
    }

    public EnvironmentalTelemetryDto ingest(Long machineId, EnvironmentalTelemetryRequest request) {
        Machine machine = machineRepository.findById(machineId)
                .orElseThrow(() -> new IllegalArgumentException("Machine not found: " + machineId));

        EnvironmentAnalysisResponse analysis = environmentAnalysisClient.analyze(
                request.getTemperature(), request.getHumidity());

        LocalDateTime now = LocalDateTime.now();

        EnvironmentalTelemetry reading = EnvironmentalTelemetry.builder()
                .machine(machine)
                .temperature(request.getTemperature())
                .humidity(request.getHumidity())
                .riskLevel(analysis.getRiskLevel())
                .timestamp(now)
                .build();
        environmentalTelemetryRepository.save(reading);

        EnvironmentalTelemetryDto dto = EnvironmentalTelemetryDto.builder()
                .machineId(machineId)
                .temperature(request.getTemperature())
                .humidity(request.getHumidity())
                .riskLevel(analysis.getRiskLevel())
                .recommendations(analysis.getRecommendations())
                .timestamp(now)
                .build();

        messagingTemplate.convertAndSend("/topic/machines/" + machineId + "/environment", dto);
        log.debug("Ingested environmental telemetry for machine {}: temp={}, humidity={}, risk={}",
                machineId, request.getTemperature(), request.getHumidity(), analysis.getRiskLevel());

        return dto;
    }

    public EnvironmentalTelemetryDto getLatest(Long machineId) {
        if (!machineRepository.existsById(machineId)) {
            throw new IllegalArgumentException("Machine not found: " + machineId);
        }

        return environmentalTelemetryRepository.findTop1ByMachine_IdOrderByTimestampDesc(machineId)
                .map(reading -> EnvironmentalTelemetryDto.builder()
                        .machineId(machineId)
                        .temperature(reading.getTemperature())
                        .humidity(reading.getHumidity())
                        .riskLevel(reading.getRiskLevel())
                        .timestamp(reading.getTimestamp())
                        .build())
                .orElseThrow(() -> new IllegalStateException("No environmental telemetry available for machine " + machineId));
    }
}
