package com.pfe.predictive.nlp.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NlpImageAnalysisDTO {

    private Long id;
    private Long machineId;
    private Long attachmentId;
    private String status;
    private String description;
    private String errorMessage;
    private String riskLevel;
    private List<String> keywords;
    private String modelVersion;
    private String modelBackend;
    private BigDecimal processingTimeMs;

    /** Chat-ready summary — mirrors NlpResponseDTO's conversational "message" field. */
    private String message;

    private LocalDateTime createdAt;
}
