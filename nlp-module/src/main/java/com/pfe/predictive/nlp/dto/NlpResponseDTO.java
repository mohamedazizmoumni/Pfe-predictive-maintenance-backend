package com.pfe.predictive.nlp.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
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
public class NlpResponseDTO {

    private Long id;

    private Long machineId;

    @JsonAlias({"rawText", "text", "reportText", "technicianReport", "report_text"})
    private String rawText;

    @JsonAlias({"failureType", "failure_type"})
    private String failureType;

    @JsonAlias({"riskLevel", "risk_level"})
    private String riskLevel;

    @JsonAlias({"keywords", "key_words"})
    @JsonDeserialize(using = FlexibleStringListDeserializer.class)
    private List<String> keywords;

    private String recommendation;

    @JsonAlias({"rootCause", "root_cause"})
    private String rootCause;

    private Double confidence;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
}
