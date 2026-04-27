package com.pfe.predictive.ml.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MlWebMvcConfig implements WebMvcConfigurer {

    private final MlRateLimitInterceptor mlRateLimitInterceptor;

    public MlWebMvcConfig(MlRateLimitInterceptor mlRateLimitInterceptor) {
        this.mlRateLimitInterceptor = mlRateLimitInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(mlRateLimitInterceptor)
                .addPathPatterns("/api/v1/ml/predict");
    }
}
