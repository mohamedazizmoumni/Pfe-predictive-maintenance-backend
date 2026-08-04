package com.pfe.predictive.nlp.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.pfe.predictive.nlp.dto.NlpRequestDTO;
import com.pfe.predictive.nlp.dto.NlpResponseDTO;
import com.pfe.predictive.nlp.exception.NlpClientException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class NlpAnalysisClient {

    private final WebClient webClient;

    public NlpAnalysisClient(@Qualifier("nlpWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    /**
     * Wire-transfer object that matches the Python NLPAnalyzeRequest schema exactly:
     * {
     *   "machineId":           <int|null>,
     *   "text":                "<string>",          ← Java "rawText" → Python "text"
     *   "source":              "<string>",
     *   "machineContext":      { ... } | null,
     *   "conversationHistory": [ ... ] | null
     * }
     *
     * Fields are null-omitted so we never send unnecessary nulls to Python.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private static final class PythonNlpRequest {
        @JsonProperty("machineId")
        private final Long machineId;

        @JsonProperty("text")
        private final String text;

        @JsonProperty("source")
        private final String source;

        @JsonProperty("machineContext")
        private final Map<String, Object> machineContext;

        @JsonProperty("conversationHistory")
        private final List<Map<String, String>> conversationHistory;

        PythonNlpRequest(NlpRequestDTO req) {
            this.machineId           = req.getMachineId();
            this.text                = req.getRawText();
            this.source              = req.getSource() != null ? req.getSource() : "technician_report";
            this.machineContext      = req.getMachineContext();
            this.conversationHistory = req.getConversationHistory();
        }
    }

    public NlpResponseDTO analyze(NlpRequestDTO request) {
        PythonNlpRequest pythonPayload = new PythonNlpRequest(request);

        log.debug("Forwarding NLP request to Python — machineId={}, hasContext={}, historySize={}",
            request.getMachineId(),
            request.getMachineContext() != null,
            request.getConversationHistory() != null ? request.getConversationHistory().size() : 0);

        try {
            NlpResponseDTO response = webClient.post()
                .uri("/nlp/analyze")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(pythonPayload)
                .retrieve()
                .onStatus(
                    status -> status.is4xxClientError() || status.is5xxServerError(),
                    clientResponse -> clientResponse.bodyToMono(String.class)
                        .defaultIfEmpty("")
                        .flatMap(body -> Mono.error(new NlpClientException(
                            "NLP service returned status " + clientResponse.statusCode().value() +
                                (body.isBlank() ? "" : ": " + body)
                        )))
                )
                .bodyToMono(NlpResponseDTO.class)
                .block(Duration.ofSeconds(20));

            if (response == null) {
                throw new NlpClientException("NLP service returned an empty response");
            }

            // Ensure rawText is populated for the mapper to persist
            if (response.getRawText() == null) {
                response.setRawText(request.getRawText());
            }

            log.info("NLP analysis for machine {}: intent={}, failureType={}, riskLevel={}, confidence={}",
                request.getMachineId(), response.getIntent(),
                response.getFailureType(), response.getRiskLevel(), response.getConfidence());

            return response;

        } catch (NlpClientException ex) {
            throw ex;
        } catch (WebClientException ex) {
            throw new NlpClientException("Failed to call NLP service: " + ex.getMessage(), ex);
        } catch (RuntimeException ex) {
            throw new NlpClientException("Unexpected NLP client error: " + ex.getMessage(), ex);
        }
    }
}
