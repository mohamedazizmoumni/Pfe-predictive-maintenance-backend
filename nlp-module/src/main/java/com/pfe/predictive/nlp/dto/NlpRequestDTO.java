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

import java.util.List;
import java.util.Map;

/**
 * Incoming NLP analysis request from the Angular frontend.
 *
 * Maps 1-to-1 to the Python NLPAnalyzeRequest schema so that all context
 * the frontend sends is forwarded to the Python service without stripping.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class NlpRequestDTO {

    @NotNull(message = "machineId is required")
    private Long machineId;

    /** The technician report / chat message text. Accepted under several aliases
     *  sent by different frontend versions. */
    @NotBlank(message = "rawText is required")
    @Size(max = 10000, message = "rawText must be at most 10000 characters")
    @JsonAlias({"rawText", "text", "reportText", "technicianReport", "report_text"})
    private String rawText;

    /**
     * Optional rich machine context forwarded by the Angular frontend:
     * health score, active alerts, sensor readings, last maintenance date, etc.
     * Python's MachineContext schema accepts:
     *   machineId, machineName, zone, status, healthScore,
     *   sensors, activeAlerts, lastMaintenance, role
     */
    private Map<String, Object> machineContext;

    /**
     * Optional conversation history for multi-turn chat.
     * Each entry: { "role": "user"|"assistant", "text": "..." }
     */
    private List<Map<String, String>> conversationHistory;

    /** Request source label forwarded to Python. Defaults to "technician_report". */
    @Builder.Default
    private String source = "technician_report";
}
