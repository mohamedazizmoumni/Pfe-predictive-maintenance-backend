package com.pfe.predictive.auth.web;

import com.pfe.predictive.auth.config.AuthRateLimitProperties;
import com.pfe.predictive.auth.exception.AuthRateLimitExceededException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Per-IP sliding-minute rate limit for unauthenticated auth endpoints
 * (login, register, face-login) to slow down credential stuffing / brute
 * force attempts. There's no user principal yet at this point, so the
 * limiter keys purely on client IP, unlike MlRateLimitInterceptor.
 */
@Component
public class AuthRateLimitInterceptor implements HandlerInterceptor {

    private final AuthRateLimitProperties properties;
    private final ConcurrentHashMap<String, CounterWindow> counters = new ConcurrentHashMap<>();

    public AuthRateLimitInterceptor(AuthRateLimitProperties properties) {
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
            throw new AuthRateLimitExceededException("Too many attempts. Please retry later.");
        }
        return true;
    }

    private String resolveKey(HttpServletRequest request) {
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
