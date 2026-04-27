package com.pfe.predictive.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    public static final String REQUEST_ID_HEADER = "X-Request-ID";
    public static final String CORRELATION_ID_ATTR = "correlationId";
    public static final String REQUEST_ID_ATTR = "requestId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String correlationId = resolveHeaderOrRandom(request.getHeader(CORRELATION_ID_HEADER));
        String requestId = resolveHeaderOrRandom(request.getHeader(REQUEST_ID_HEADER));

        request.setAttribute(CORRELATION_ID_ATTR, correlationId);
        request.setAttribute(REQUEST_ID_ATTR, requestId);
        response.setHeader(CORRELATION_ID_HEADER, correlationId);
        response.setHeader(REQUEST_ID_HEADER, requestId);

        MDC.put(CORRELATION_ID_ATTR, correlationId);
        MDC.put(REQUEST_ID_ATTR, requestId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(CORRELATION_ID_ATTR);
            MDC.remove(REQUEST_ID_ATTR);
        }
    }

    private String resolveHeaderOrRandom(String value) {
        if (value != null && !value.isBlank()) {
            return value;
        }
        return UUID.randomUUID().toString();
    }
}
