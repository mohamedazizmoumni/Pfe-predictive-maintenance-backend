package com.pfe.predictive.nlp.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Wire shape returned by ml_service's POST /vision/analyze-equipment. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class NlpImageAnalysisClientResponse {

    private String description;
    private String riskLevel;
    private List<String> keywords;
    private String modelVersion;
    private String modelBackend;
    private BigDecimal processingTimeMs;
    private String message;
}
