package com.pfe.predictive.ml.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class MlHttpClientConfig {

    @Bean
    RestClient pythonMlRestClient(MlServiceProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) properties.getTimeout().getConnect().toMillis());
        requestFactory.setReadTimeout((int) properties.getTimeout().getRead().toMillis());

        RestClient.Builder builder = RestClient.builder()
                .requestFactory(requestFactory)
                .baseUrl(properties.getBaseUrl());

        if (!properties.getAuth().getHeaderName().isBlank() && !properties.getAuth().getHeaderValue().isBlank()) {
            builder.defaultHeader(properties.getAuth().getHeaderName(), properties.getAuth().getHeaderValue());
        }

        return builder.build();
    }
}
