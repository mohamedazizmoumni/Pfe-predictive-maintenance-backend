package com.pfe.predictive.nlp.vision;

import com.pfe.predictive.nlp.dto.NlpImageAnalysisDTO;
import com.pfe.predictive.nlp.entity.NlpImageAnalysis;
import org.springframework.util.StringUtils;

final class NlpImageAnalysisMapper {

    private NlpImageAnalysisMapper() {
    }

    static NlpImageAnalysisDTO toDto(NlpImageAnalysis saved, String message) {
        return NlpImageAnalysisDTO.builder()
                .id(saved.getId())
                .machineId(saved.getMachineId())
                .attachmentId(saved.getAttachmentId())
                .status(saved.getStatus())
                .description(saved.getDescription())
                .errorMessage(saved.getErrorMessage())
                .riskLevel(saved.getRiskLevel())
                .keywords(saved.getKeywords())
                .modelVersion(saved.getModelVersion())
                .modelBackend(saved.getModelBackend())
                .processingTimeMs(saved.getProcessingTimeMs())
                .message(StringUtils.hasText(message) ? message : saved.getDescription())
                .createdAt(saved.getCreatedAt())
                .build();
    }
}
