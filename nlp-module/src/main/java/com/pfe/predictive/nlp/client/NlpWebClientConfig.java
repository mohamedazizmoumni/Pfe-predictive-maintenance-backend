package com.pfe.predictive.nlp.client;

import io.netty.channel.ChannelOption;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Configuration
public class NlpWebClientConfig {

    @Bean(name = "nlpWebClient")
    public WebClient nlpWebClient(
            WebClient.Builder builder,
            @Value("${nlp.service.url:http://localhost:8000}") String baseUrl,
            @Value("${nlp.service.connect-timeout-ms:5000}") int connectTimeoutMs,
            @Value("${nlp.service.read-timeout-ms:15000}") int readTimeoutMs,
            @Value("${ml.internal-api-key:sentinel-dev-secret}") String internalApiKey) {

        HttpClient httpClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMs)
            .responseTimeout(Duration.ofMillis(readTimeoutMs));

        return builder
            .baseUrl(baseUrl)
            .defaultHeader("X-Internal-Key", internalApiKey)
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .build();
    }

    /**
     * Separate client (and separate, much longer timeout) for the
     * equipment-photo vision endpoint. Measured CPU-only inference for the
     * local vision model runs several minutes per photo on modest hardware —
     * nothing like the text-classification pipeline the default
     * nlpWebClient's 15s timeout is tuned for. This is only safe because
     * the call happens on a background thread (NlpImageAnalysisService runs
     * it @Async, after already returning a fast HTTP response to the
     * caller) — a request-thread-blocking call would never use a timeout
     * this long. 420s (7 min) covers the ~4-5 min observed worst case with
     * headroom.
     */
    @Bean(name = "nlpVisionWebClient")
    public WebClient nlpVisionWebClient(
            WebClient.Builder builder,
            @Value("${nlp.service.url:http://localhost:8000}") String baseUrl,
            @Value("${nlp.vision.connect-timeout-ms:5000}") int connectTimeoutMs,
            @Value("${nlp.vision.read-timeout-ms:420000}") int readTimeoutMs,
            @Value("${ml.internal-api-key:sentinel-dev-secret}") String internalApiKey) {

        HttpClient httpClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMs)
            .responseTimeout(Duration.ofMillis(readTimeoutMs));

        return builder
            .baseUrl(baseUrl)
            .defaultHeader("X-Internal-Key", internalApiKey)
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .build();
    }
}
