package com.pfe.predictive.environment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Environmental reading with risk assessment, for REST responses and WebSocket broadcast")
public class EnvironmentalTelemetryDto {

    private Long machineId;
    private Double temperature;
    private Double humidity;
    private String riskLevel;
    private List<String> recommendations;
    private LocalDateTime timestamp;
}
