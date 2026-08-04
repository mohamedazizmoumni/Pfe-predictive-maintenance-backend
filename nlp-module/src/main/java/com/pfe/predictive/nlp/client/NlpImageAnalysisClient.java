package com.pfe.predictive.nlp.client;

import com.pfe.predictive.nlp.exception.NlpClientException;
import java.time.Duration;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class NlpImageAnalysisClient {

    private final WebClient webClient;
    private final Duration readTimeout;

    public NlpImageAnalysisClient(
            @Qualifier("nlpVisionWebClient") WebClient webClient,
            @Value("${nlp.vision.read-timeout-ms:420000}") long readTimeoutMs) {
        this.webClient = webClient;
        this.readTimeout = Duration.ofMillis(readTimeoutMs);
    }

    /**
     * Takes raw bytes rather than a MultipartFile — this call runs on a
     * background thread well after the original upload request has
     * completed (see NlpImageAnalysisService), and a servlet-backed
     * MultipartFile's underlying temp file is not guaranteed to survive
     * past that request's lifecycle.
     */
    public NlpImageAnalysisClientResponse analyze(
            byte[] imageBytes, String filename, String contentType, Long machineId, String context) {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        try {
            builder.part("image", new ByteArrayResource(imageBytes) {
                        @Override
                        public String getFilename() {
                            return filename != null ? filename : "photo.jpg";
                        }
                    })
                    .filename(filename != null ? filename : "photo.jpg")
                    .contentType(MediaType.parseMediaType(contentType != null ? contentType : "image/jpeg"));
        } catch (IllegalArgumentException ex) {
            throw new NlpClientException("Invalid image content type: " + ex.getMessage(), ex);
        }

        log.debug("Forwarding equipment photo to vision service — machineId={}, size={} bytes",
            machineId, imageBytes.length);

        try {
            NlpImageAnalysisClientResponse response = webClient.post()
                .uri(uriBuilder -> uriBuilder.path("/vision/analyze-equipment")
                    .queryParamIfPresent("machineId", Optional.ofNullable(machineId))
                    .queryParamIfPresent("context", Optional.ofNullable(context))
                    .build())
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .accept(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromMultipartData(builder.build()))
                .retrieve()
                .onStatus(
                    status -> status.is4xxClientError() || status.is5xxServerError(),
                    clientResponse -> clientResponse.bodyToMono(String.class)
                        .defaultIfEmpty("")
                        .flatMap(body -> Mono.error(new NlpClientException(
                            "Vision service returned status " + clientResponse.statusCode().value() +
                                (body.isBlank() ? "" : ": " + body)
                        )))
                )
                .bodyToMono(NlpImageAnalysisClientResponse.class)
                .block(readTimeout);

            if (response == null) {
                throw new NlpClientException("Vision service returned an empty response");
            }

            log.info("Vision analysis for machine {}: riskLevel={}, backend={}, processingTimeMs={}",
                machineId, response.getRiskLevel(), response.getModelBackend(), response.getProcessingTimeMs());

            return response;

        } catch (NlpClientException ex) {
            throw ex;
        } catch (WebClientException ex) {
            throw new NlpClientException("Failed to call vision service: " + ex.getMessage(), ex);
        } catch (RuntimeException ex) {
            throw new NlpClientException("Unexpected vision client error: " + ex.getMessage(), ex);
        }
    }
}
