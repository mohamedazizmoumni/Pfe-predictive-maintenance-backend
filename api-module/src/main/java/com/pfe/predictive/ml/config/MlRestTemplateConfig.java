package com.pfe.predictive.ml.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration for RestTemplate beans that call FastAPI ML microservices.
 * 
 * Adds X-Internal-Key header interceptor for authentication to internal services.
 * All ML service HTTP calls use these beans to ensure consistent security headers.
 */
@Configuration
public class MlRestTemplateConfig {

    @Value("${ml.internal-api-key:sentinel-dev-secret}")
    private String internalApiKey;

    @Value("${ml.predictive.url:http://localhost:5000}")
    private String predictiveUrl;

    @Value("${ml.face.url:http://localhost:5001}")
    private String faceUrl;

    /**
     * RestTemplate bean for predictive maintenance service (port 5000).
     * All requests to this service will include X-Internal-Key header.
     */
    @Bean(name = "mlRestTemplate")
    public RestTemplate mlRestTemplate() {
        return new RestTemplateBuilder()
                .requestFactory(this::clientHttpRequestFactory)
                .interceptors((request, body, execution) -> {
                    request.getHeaders().set("X-Internal-Key", internalApiKey);
                    return execution.execute(request, body);
                })
                .build();
    }

    /**
     * RestTemplate bean for face recognition service (port 5001).
     * All requests to this service will include X-Internal-Key header.
     */
    @Bean(name = "faceRestTemplate")
    public RestTemplate faceRestTemplate() {
        return new RestTemplateBuilder()
                .requestFactory(this::clientHttpRequestFactory)
                .interceptors((request, body, execution) -> {
                    request.getHeaders().set("X-Internal-Key", internalApiKey);
                    return execution.execute(request, body);
                })
                .build();
    }

    /**
     * Shared HTTP request factory with connection and read timeouts.
     */
    private ClientHttpRequestFactory clientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);  // 5 seconds
        factory.setReadTimeout(10000);    // 10 seconds
        return factory;
    }
}
