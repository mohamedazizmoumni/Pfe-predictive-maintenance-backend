package com.pfe.predictive.chatbot;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class ChatbotAuthorizationService {

    private static final Set<String> SENSITIVE_TERMS = Set.of(
            "password",
            "passwd",
            "secret",
            "api key",
            "token",
            "private key",
            "credential",
            "connection string",
            "jwt",
            "access key"
    );

    private static final Set<String> PRIVILEGED_TERMS = Set.of(
            "create user",
            "delete user",
            "assign role",
            "permissions",
            "permission",
            "security config",
            "database config",
            "system config",
            "grant access",
            "revoke access"
    );

        private static final Set<String> VIEWER_FORBIDDEN_ACTIONS = Set.of(
            "create",
            "update",
            "delete",
            "remove",
            "drop",
            "truncate",
            "assign",
            "configure"
        );

        private final Map<String, List<String>> roleKeywords = new HashMap<>();

        private static final Map<String, Set<String>> DOMAIN_KEYWORDS = Map.of(
            "USER_ADMIN", Set.of("user", "users", "role", "roles", "permission", "permissions", "admin", "account"),
            "INVENTORY", Set.of("inventory", "stock", "part", "reorder", "supplier", "warehouse"),
            "MACHINE", Set.of("machine", "sensor", "equipment", "asset"),
            "MAINTENANCE", Set.of("maintenance", "work order", "task", "repair", "schedule"),
            "ALERT", Set.of("alert", "alarm", "incident", "notification"),
            "PREDICTION", Set.of("prediction", "model", "dataset", "feature", "anomaly", "analytics"),
            "DASHBOARD", Set.of("dashboard", "kpi", "report", "overview", "summary")
        );

        private static final Map<String, Set<String>> ROLE_ALLOWED_DOMAINS = Map.of(
            "MANAGER", Set.of("INVENTORY", "MACHINE", "MAINTENANCE", "ALERT", "PREDICTION", "DASHBOARD"),
            "TECHNICIAN", Set.of("INVENTORY", "MACHINE", "MAINTENANCE", "ALERT", "DASHBOARD"),
            "DATA_SCIENTIST", Set.of("PREDICTION", "MACHINE", "ALERT", "DASHBOARD"),
            "STOCK_MANAGER", Set.of("INVENTORY", "DASHBOARD"),
            "VIEWER", Set.of("DASHBOARD", "MACHINE", "ALERT")
        );

    public ChatbotAuthorizationService() {
        roleKeywords.put("SUPER_ADMIN", List.of("all"));
        roleKeywords.put("ADMIN", List.of("all"));

        roleKeywords.put("MANAGER", List.of(
                "dashboard", "kpi", "report", "maintenance", "alert", "inventory", "machine", "prediction"
        ));

        roleKeywords.put("TECHNICIAN", List.of(
                "maintenance", "machine", "sensor", "alert", "inventory", "task", "work order"
        ));

        roleKeywords.put("DATA_SCIENTIST", List.of(
                "prediction", "model", "dataset", "feature", "anomaly", "sensor", "analytics"
        ));

        roleKeywords.put("STOCK_MANAGER", List.of(
                "inventory", "stock", "part", "reorder", "supplier", "warehouse"
        ));

        roleKeywords.put("VIEWER", List.of(
                "dashboard", "overview", "report", "status", "summary", "machine", "alert"
        ));
    }

    public boolean isAuthorized(Set<String> roles, String question) {
        String normalizedQuestion = normalize(question);

        if (containsAny(normalizedQuestion, SENSITIVE_TERMS)) {
            return false;
        }

        if (isAdminLike(roles)) {
            return true;
        }

        if (containsAny(normalizedQuestion, PRIVILEGED_TERMS)) {
            return false;
        }

        if (roles.contains("VIEWER") && containsAny(normalizedQuestion, VIEWER_FORBIDDEN_ACTIONS)) {
            return false;
        }

        Set<String> requestedDomains = detectDomains(normalizedQuestion);
        if (requestedDomains.isEmpty()) {
            // General question without a protected domain mention -> allow.
            return true;
        }

        Set<String> allowedDomains = collectAllowedDomains(roles);
        if (allowedDomains.isEmpty()) {
            return false;
        }

        return allowedDomains.containsAll(requestedDomains);
    }

    private Set<String> collectAllowedKeywords(Set<String> roles) {
        Set<String> keywords = new HashSet<>();
        for (String role : roles) {
            List<String> roleAllowedKeywords = roleKeywords.getOrDefault(role, List.of());
            keywords.addAll(roleAllowedKeywords);
        }
        return keywords;
    }

    private Set<String> collectAllowedDomains(Set<String> roles) {
        Set<String> domains = new HashSet<>();
        for (String role : roles) {
            domains.addAll(ROLE_ALLOWED_DOMAINS.getOrDefault(role, Set.of()));
        }
        return domains;
    }

    private Set<String> detectDomains(String normalizedQuestion) {
        Set<String> domains = new HashSet<>();
        for (Map.Entry<String, Set<String>> entry : DOMAIN_KEYWORDS.entrySet()) {
            if (containsAny(normalizedQuestion, entry.getValue())) {
                domains.add(entry.getKey());
            }
        }
        return domains;
    }

    private boolean isAdminLike(Set<String> roles) {
        return roles.contains("ADMIN") || roles.contains("SUPER_ADMIN");
    }

    private boolean containsAny(String text, Set<String> candidates) {
        for (String candidate : candidates) {
            if (text.contains(candidate)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String input) {
        return input == null ? "" : input.toLowerCase(Locale.ROOT);
    }
}
