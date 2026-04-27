package com.pfe.predictive.security;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.*;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Aspect
@Component
public class SentinelSecurityAspect {

    @Before("@annotation(com.pfe.predictive.security.SentinelSecured)")
    public void check(JoinPoint joinPoint) throws Throwable {

        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();

        SentinelSecured annotation = method.getAnnotation(SentinelSecured.class);

        if (annotation == null) {
            annotation = joinPoint.getTarget().getClass()
                    .getAnnotation(SentinelSecured.class);
        }

        if (annotation == null) return;

        var auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Not authenticated");
        }

        String expression = annotation.value().expression;

        var authorities = auth.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .toList();

        boolean allowed = evaluate(expression, authorities);

        if (!allowed) {
            throw new AccessDeniedException("Access denied");
        }
    }

    private boolean evaluate(String expr, java.util.List<String> authorities) {

        Set<String> authoritySet = authorities.stream()
                .map(String::trim)
                .collect(Collectors.toSet());

        Set<String> roleNormalizedAuthorities = authorities.stream()
                .map(this::normalizeRoleAuthority)
                .collect(Collectors.toSet());

        if (expr.contains("hasAnyRole")) {
            return parseArgs(expr).stream()
                    .map(this::normalizeRoleAuthority)
                    .anyMatch(roleNormalizedAuthorities::contains);
        }

        if (expr.contains("hasRole")) {
            String role = parseArgs(expr).stream().findFirst().orElse("");
            return roleNormalizedAuthorities.contains(normalizeRoleAuthority(role));
        }

        if (expr.contains("hasAnyAuthority")) {
            return parseArgs(expr).stream().anyMatch(arg ->
                    authoritySet.contains(arg) || roleNormalizedAuthorities.contains(normalizeRoleAuthority(arg)));
        }

        if (expr.contains("hasAuthority")) {
            String authority = parseArgs(expr).stream().findFirst().orElse("");
            return authoritySet.contains(authority)
                    || roleNormalizedAuthorities.contains(normalizeRoleAuthority(authority));
        }

        return false;
    }

    private List<String> parseArgs(String expr) {
        if (expr == null || !expr.contains("(") || !expr.contains(")")) {
            return List.of();
        }

        String raw = expr.substring(expr.indexOf("(") + 1, expr.lastIndexOf(")"));
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .map(s -> s.replace("'", "").replace("\"", ""))
                .filter(s -> !s.isBlank())
                .toList();
    }

    private String normalizeRoleAuthority(String roleOrAuthority) {
        if (roleOrAuthority == null) {
            return "";
        }

        String cleaned = roleOrAuthority.trim()
                .replace("[", "")
                .replace("]", "")
                .replace("\"", "");

        if (cleaned.isBlank()) {
            return "";
        }

        return cleaned.startsWith("ROLE_") ? cleaned : "ROLE_" + cleaned;
    }
}