package com.pfe.predictive.nlp.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class NlpRequestDTO {

    @NotNull(message = "machineId is required")
    private Long machineId;

    @NotBlank(message = "rawText is required")
    @Size(max = 10000, message = "rawText must be at most 10000 characters")
    @JsonAlias({"rawText", "text", "reportText", "technicianReport", "report_text"})
    private String rawText;
}
