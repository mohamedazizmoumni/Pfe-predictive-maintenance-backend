package com.pfe.predictive.ml.web;

import com.pfe.predictive.ml.config.MlRateLimitProperties;
import com.pfe.predictive.ml.exception.MlRateLimitExceededException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class MlRateLimitInterceptor implements HandlerInterceptor {

    private final MlRateLimitProperties properties;
    private final ConcurrentHashMap<String, CounterWindow> counters = new ConcurrentHashMap<>();

    public MlRateLimitInterceptor(MlRateLimitProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!properties.isEnabled()) {
            return true;
        }

        String key = resolveKey(request);
        long nowMillis = Instant.now().toEpochMilli();
        long windowStart = nowMillis - (nowMillis % 60_000L);

        CounterWindow window = counters.compute(key, (k, existing) -> {
            if (existing == null || existing.windowStartMillis != windowStart) {
                return new CounterWindow(windowStart);
            }
            return existing;
        });

        int value = window.counter.incrementAndGet();
        if (value > properties.getRequestsPerMinute()) {
            throw new MlRateLimitExceededException("Prediction rate limit exceeded. Please retry later.");
        }
        return true;
    }

    private String resolveKey(HttpServletRequest request) {
        String user = request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : null;
        if (user != null && !user.isBlank()) {
            return "user:" + user;
        }

        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return "ip:" + forwarded.split(",")[0].trim();
        }

        return "ip:" + request.getRemoteAddr();
    }

    private static final class CounterWindow {
        private final long windowStartMillis;
        private final AtomicInteger counter = new AtomicInteger(0);

        private CounterWindow(long windowStartMillis) {
            this.windowStartMillis = windowStartMillis;
        }
    }
}
