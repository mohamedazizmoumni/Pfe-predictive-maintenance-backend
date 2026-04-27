package com.pfe.predictive.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class FaceLoginHttpClientConfig {

    @Bean
    RestTemplate faceLoginRestTemplate(FaceLoginProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) properties.getTimeout().getConnect().toMillis());
        requestFactory.setReadTimeout((int) properties.getTimeout().getRead().toMillis());

        return new RestTemplate(requestFactory);
    }
}
