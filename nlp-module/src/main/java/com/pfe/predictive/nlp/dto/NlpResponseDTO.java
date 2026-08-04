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

/**
 * NLP analysis response — mirrors Python's NLPAnalyzeResponse schema
 * so that every field Python returns reaches the Angular frontend.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class NlpResponseDTO {

    // ── persistence fields (set after DB save) ──────────────────────────────
    private Long id;

    private Long machineId;

    @JsonAlias({"rawText", "text", "reportText", "technicianReport", "report_text"})
    private String rawText;

    // ── Python classifier fields ─────────────────────────────────────────────
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

    /** The cleaned/normalised input text that Python processed. */
    private String cleanedText;

    /** Python model version string. */
    private String modelVersion;

    /** How long Python took to process the request (milliseconds). */
    private Double processingTimeMs;

    // ── Conversational / chat fields (Python NLPAnalyzeResponse additions) ───

    /**
     * Human-readable chat reply.
     * The Angular AI Copilot component displays this as the assistant's message
     * when the user asks a question rather than submitting a symptom report.
     */
    private String message;

    /**
     * Detected intent: DIAGNOSTIC | QUESTION | STATUS | COMMAND | UNKNOWN
     */
    private String intent;

    /**
     * True when Python determined the input was a question rather than
     * a symptom/failure description.
     */
    private Boolean isQuestion;

    // ── timestamp (set after DB save) ────────────────────────────────────────
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
}
