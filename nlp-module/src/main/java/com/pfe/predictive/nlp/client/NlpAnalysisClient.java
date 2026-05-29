package com.pfe.predictive.nlp.client;

import com.pfe.predictive.nlp.dto.NlpRequestDTO;
import com.pfe.predictive.nlp.dto.NlpResponseDTO;
import com.pfe.predictive.nlp.exception.NlpClientException;
import java.time.Duration;
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

    public NlpResponseDTO analyze(NlpRequestDTO request) {
        try {
            NlpResponseDTO response = webClient.post()
                .uri("/nlp/analyze")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), clientResponse ->
                    clientResponse.bodyToMono(String.class)
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

            return response;
        } catch (NlpClientException ex) {
            throw ex;
        } catch (WebClientException ex) {
            throw new NlpClientException("Failed to call NLP service", ex);
        } catch (RuntimeException ex) {
            throw new NlpClientException("Unexpected NLP client error", ex);
        }
    }
}
