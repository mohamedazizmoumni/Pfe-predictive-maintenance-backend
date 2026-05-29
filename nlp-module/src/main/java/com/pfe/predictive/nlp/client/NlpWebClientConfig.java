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
            @Value("${nlp.service.read-timeout-ms:15000}") int readTimeoutMs) {

        HttpClient httpClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMs)
            .responseTimeout(Duration.ofMillis(readTimeoutMs));

        return builder
            .baseUrl(baseUrl)
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .build();
    }
}
