package com.pfe.predictive.nlp.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
@JsonIgnoreProperties(ignoreUnknown = true)
public class NlpAnalysisDTO {

    private Long id;

    private Long machineId;

    @JsonAlias({"failureType", "failure_type"})
    private String failureType;

    @JsonAlias({"riskLevel", "risk_level"})
    private String riskLevel;

    private List<String> keywords;

    private String recommendation;

    private String rootCause;

    private Double confidence;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
}
