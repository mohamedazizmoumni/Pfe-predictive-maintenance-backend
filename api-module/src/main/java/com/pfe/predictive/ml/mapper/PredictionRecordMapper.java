package com.pfe.predictive.ml.mapper;

import com.pfe.predictive.core.entity.PredictionRecord;
import com.pfe.predictive.ml.dto.PredictionRecordDTO;
import com.pfe.predictive.ml.dto.PredictionRecordDetailDTO;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for converting PredictionRecord entities to DTOs.
 */
@Component
public class PredictionRecordMapper {

    /**
     * Convert entity to DTO (without feature summary).
     */
    public PredictionRecordDTO toDTO(PredictionRecord entity) {
        if (entity == null) {
            return null;
        }
        return PredictionRecordDTO.builder()
                .id(entity.getId())
                .machineId(entity.getMachineId())
                .predictedAt(entity.getPredictedAt())
                .rulValue(entity.getRulValue())
                .confidenceLow(entity.getConfidenceLow())
                .confidenceHigh(entity.getConfidenceHigh())
                .riskLevel(entity.getRiskLevel())
                .modelVersion(entity.getModelVersion())
                .triggeredBy(entity.getTriggeredBy())
                .createdDate(entity.getCreatedDate())
                .build();
    }

    /**
     * Convert entity to detailed DTO (with feature summary).
     */
    public PredictionRecordDetailDTO toDetailDTO(PredictionRecord entity) {
        if (entity == null) {
            return new PredictionRecordDetailDTO();
        }
        return PredictionRecordDetailDTO.builder()
                .id(entity.getId())
                .machineId(entity.getMachineId())
                .predictedAt(entity.getPredictedAt())
                .rulValue(entity.getRulValue())
                .confidenceLow(entity.getConfidenceLow())
                .confidenceHigh(entity.getConfidenceHigh())
                .riskLevel(entity.getRiskLevel())
                .modelVersion(entity.getModelVersion())
                .triggeredBy(entity.getTriggeredBy())
                .inputFeaturesSummary(entity.getInputFeaturesSummary())
                .createdDate(entity.getCreatedDate())
                .build();
    }

    /**
     * Convert list of entities to DTOs.
     */
    public List<PredictionRecordDTO> toDTOList(List<PredictionRecord> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
}
