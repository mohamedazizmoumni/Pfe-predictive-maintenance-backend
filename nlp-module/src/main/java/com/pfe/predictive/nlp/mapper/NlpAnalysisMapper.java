package com.pfe.predictive.nlp.mapper;

import com.pfe.predictive.nlp.dto.NlpAnalysisDTO;
import com.pfe.predictive.nlp.dto.NlpRequestDTO;
import com.pfe.predictive.nlp.dto.NlpResponseDTO;
import com.pfe.predictive.nlp.entity.NlpAnalysis;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class NlpAnalysisMapper {

    public NlpAnalysis toEntity(NlpRequestDTO request, NlpResponseDTO response) {
        if (request == null) {
            return null;
        }

        List<String> keywords = response != null ? normalizeKeywords(response.getKeywords()) : List.of();

        return NlpAnalysis.builder()
            .machineId(request.getMachineId())
            .rawText(resolveRawText(request, response))
            .failureType(response != null ? response.getFailureType() : null)
            .riskLevel(response != null ? response.getRiskLevel() : null)
            .keywords(new ArrayList<>(keywords))
            .recommendation(response != null ? response.getRecommendation() : null)
            .rootCause(response != null ? response.getRootCause() : null)
            .confidence(response != null && response.getConfidence() != null ? BigDecimal.valueOf(response.getConfidence()) : BigDecimal.ZERO)
            .build();
    }

    public NlpResponseDTO toResponse(NlpAnalysis analysis) {
        if (analysis == null) {
            return null;
        }

        return NlpResponseDTO.builder()
            .id(analysis.getId())
            .machineId(analysis.getMachineId())
            .rawText(analysis.getRawText())
            .failureType(analysis.getFailureType())
            .riskLevel(analysis.getRiskLevel())
            .keywords(normalizeKeywords(analysis.getKeywords()))
            .recommendation(analysis.getRecommendation())
            .rootCause(analysis.getRootCause())
            .confidence(analysis.getConfidence() != null ? analysis.getConfidence().doubleValue() : null)
            .createdAt(analysis.getCreatedAt())
            .build();
    }

    public NlpAnalysisDTO toAnalysisDto(NlpAnalysis analysis) {
        if (analysis == null) {
            return null;
        }

        return NlpAnalysisDTO.builder()
            .id(analysis.getId())
            .machineId(analysis.getMachineId())
            .failureType(analysis.getFailureType())
            .riskLevel(analysis.getRiskLevel())
            .keywords(normalizeKeywords(analysis.getKeywords()))
            .recommendation(analysis.getRecommendation())
            .rootCause(analysis.getRootCause())
            .confidence(analysis.getConfidence() != null ? analysis.getConfidence().doubleValue() : null)
            .createdAt(analysis.getCreatedAt())
            .build();
    }

    public Page<NlpAnalysisDTO> toAnalysisPage(Page<NlpAnalysis> page) {
        if (page == null) {
            return Page.empty();
        }

        return new PageImpl<>(
            page.getContent().stream().map(this::toAnalysisDto).toList(),
            page.getPageable(),
            page.getTotalElements()
        );
    }

    private String resolveRawText(NlpRequestDTO request, NlpResponseDTO response) {
        if (response != null && StringUtils.hasText(response.getRawText())) {
            return response.getRawText();
        }
        return request.getRawText();
    }

    private List<String> normalizeKeywords(List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return List.of();
        }
        return keywords.stream()
            .filter(StringUtils::hasText)
            .map(String::trim)
            .toList();
    }
}
