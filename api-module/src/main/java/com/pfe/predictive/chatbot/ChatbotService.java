package com.pfe.predictive.chatbot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pfe.predictive.alert.entity.Alert;
import com.pfe.predictive.alert.entity.AlertStatus;
import com.pfe.predictive.alert.repository.AlertRepository;
import com.pfe.predictive.calendar.dto.CalendarEventResponse;
import com.pfe.predictive.calendar.service.CalendarEventService;
import com.pfe.predictive.chatbot.dto.ChatbotResponse;
import com.pfe.predictive.core.entity.Maintenance;
import com.pfe.predictive.core.entity.MaintenanceStatus;
import com.pfe.predictive.core.entity.User;
import com.pfe.predictive.data.repository.MaintenanceRepository;
import com.pfe.predictive.data.repository.MachineRepository;
import com.pfe.predictive.data.repository.UserRepository;
import com.pfe.predictive.inventory.dto.LowStockAlertResponse;
import com.pfe.predictive.inventory.repository.PartRepository;
import com.pfe.predictive.inventory.entity.PartStatus;
import com.pfe.predictive.inventory.entity.ReorderStatus;
import com.pfe.predictive.inventory.entity.StockOrderStatus;
import com.pfe.predictive.inventory.service.InventoryAnalyticsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.security.KeyStore;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ChatbotService {

    private static final String NOT_AUTHORIZED = "NOT AUTHORIZED";
        private static final List<String> GENERIC_NON_PLATFORM_PHRASES = List.of(
            "i don't have access to the specific rules",
            "i dont have access to the specific rules",
            "however, in general",
            "here are some tips",
            "best practice",
            "strict security protocols",
            "ensure confidentiality",
            "as an ai assistant"
        );

    private final ObjectMapper objectMapper;
    private final ChatbotAuthorizationService authorizationService;
    private final InventoryAnalyticsService inventoryAnalyticsService;
    private final UserRepository userRepository;
    private final MachineRepository machineRepository;
    private final PartRepository partRepository;
    private final AlertRepository alertRepository;
    private final MaintenanceRepository maintenanceRepository;
    private final CalendarEventService calendarEventService;

    private static final List<String> KNOWN_ROLES = List.of(
            "SUPER_ADMIN",
            "ADMIN",
            "MANAGER",
            "TECHNICIAN",
            "STOCK_MANAGER"
    );

    @Value("${openai.api.key:${OPENAI_API_KEY:}}")
    private String openAiApiKey;

    @Value("${openai.api.url:https://api.openai.com/v1/chat/completions}")
    private String openAiApiUrl;

    @Value("${openai.model:gpt-4o-mini}")
    private String openAiModel;

    @Value("${chatbot.timeout-seconds:30}")
    private int timeoutSeconds;

    @Value("${chatbot.ollama.enabled:false}")
    private boolean ollamaEnabled;

    @Value("${chatbot.ollama.api-url:http://127.0.0.1:11434/api/generate}")
    private String ollamaApiUrl;

    @Value("${chatbot.ollama.model:tinyllama}")
    private String ollamaModel;

    public ChatbotService(
            ObjectMapper objectMapper,
            ChatbotAuthorizationService authorizationService,
            InventoryAnalyticsService inventoryAnalyticsService,
            UserRepository userRepository,
            MachineRepository machineRepository,
            PartRepository partRepository,
            AlertRepository alertRepository,
            MaintenanceRepository maintenanceRepository,
            CalendarEventService calendarEventService
    ) {
        this.objectMapper = objectMapper;
        this.authorizationService = authorizationService;
        this.inventoryAnalyticsService = inventoryAnalyticsService;
        this.userRepository = userRepository;
        this.machineRepository = machineRepository;
        this.partRepository = partRepository;
        this.alertRepository = alertRepository;
        this.maintenanceRepository = maintenanceRepository;
        this.calendarEventService = calendarEventService;
    }

    public ChatbotResponse askQuestion(String question, Authentication authentication) {
        Set<String> roles = extractRoles(authentication);
        List<String> roleList = roles.stream().sorted().toList();

        if (!authorizationService.isAuthorized(roles, question)) {
            return new ChatbotResponse(NOT_AUTHORIZED, false, roleList);
        }

        // Checked before the generic platform-wide shortcuts below: "my tasks"/
        // "what's assigned to me" must answer from the CALLER's own data, not
        // a platform-wide count. Every shortcut past this point is identity-
        // agnostic — two different users with the same role asking the exact
        // same generic question get an identical answer, which is fine for
        // "how many machines exist" but wrong for "how many tasks do I have".
        if (isPersonalScopeQuestion(question)) {
            String personalAnswer = buildPersonalDirectAnswer(question, authentication);
            if (personalAnswer != null) {
                return new ChatbotResponse(personalAnswer, true, roleList);
            }
        }

        String platformDirectAnswer = buildPlatformDirectAnswer(question);
        if (platformDirectAnswer != null) {
            return new ChatbotResponse(platformDirectAnswer, true, roleList);
        }

        if (isLowStockQuestion(question)) {
            String directAnswer = buildLowStockDirectAnswer();
            return new ChatbotResponse(directAnswer, true, roleList);
        }

        if (isUsersByRoleQuestion(question)) {
            String directAnswer = buildUsersByRoleDirectAnswer(question);
            return new ChatbotResponse(directAnswer, true, roleList);
        }

        String resolvedApiKey = resolveApiKey();

        if (resolvedApiKey == null || resolvedApiKey.isBlank()) {
            if (!ollamaEnabled) {
                throw new IllegalStateException("OpenAI API key is not configured. Set OPENAI_API_KEY or openai.api.key");
            }

            String localAnswer = callOllama(question, roleList);
            return new ChatbotResponse(localAnswer, true, roleList);
        }

        try {
            String answer = callOpenAi(question, roleList, resolvedApiKey);
            return new ChatbotResponse(applyResponseGuardrails(question, answer), true, roleList);
        } catch (IllegalStateException ex) {
            if (ollamaEnabled && shouldFallbackToOllama(ex)) {
                String localAnswer = callOllama(question, roleList);
                return new ChatbotResponse(applyResponseGuardrails(question, localAnswer), true, roleList);
            }
            throw ex;
        }
    }

    private String applyResponseGuardrails(String question, String answer) {
        String clean = answer == null ? "" : answer.trim();
        if (clean.isBlank()) {
            return "No data available.";
        }

        String normalized = normalizeText(clean);
        if (normalized.equals("not authorized") || normalized.equals("not authorised")) {
            return NOT_AUTHORIZED;
        }

        if (containsGenericNonPlatformContent(normalized)) {
            String direct = buildPlatformDirectAnswer(question);
            if (direct != null && !direct.isBlank()) {
                return direct;
            }

            if (isLowStockQuestion(question)) {
                return buildLowStockDirectAnswer();
            }

            if (isUsersByRoleQuestion(question)) {
                return buildUsersByRoleDirectAnswer(question);
            }

            return "No data available.";
        }

        return clean;
    }

    private boolean containsGenericNonPlatformContent(String normalizedAnswer) {
        if (normalizedAnswer == null || normalizedAnswer.isBlank()) {
            return false;
        }

        for (String phrase : GENERIC_NON_PLATFORM_PHRASES) {
            if (normalizedAnswer.contains(phrase)) {
                return true;
            }
        }

        return false;
    }

    private boolean shouldFallbackToOllama(IllegalStateException ex) {
        String message = ex.getMessage();
        if (message == null) {
            return false;
        }

        return message.contains("status 429")
                || message.contains("status 401")
                || message.contains("status 403");
    }

    private String callOpenAi(String question, List<String> roles, String apiKey) {
        String systemPrompt = buildSystemPrompt(roles);

        Map<String, Object> payload = new HashMap<>();
        payload.put("model", openAiModel);
        payload.put("temperature", 0.2);
        payload.put("max_tokens", 350);
        payload.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", question)
        ));

        try {
            String requestBody = objectMapper.writeValueAsString(payload);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(openAiApiUrl))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpClient client = buildOutboundHttpsClient();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                throw new IllegalStateException("OpenAI request failed with status " + response.statusCode());
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
            String content = contentNode.asText("").trim();

            if (content.isEmpty()) {
                throw new IllegalStateException("Empty response from OpenAI");
            }

            return content;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to serialize or parse OpenAI payload", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("OpenAI request was interrupted", e);
        }
    }

    private static final boolean IS_WINDOWS =
            System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");

    /**
     * On this network, HTTPS calls out to api.openai.com fail the JVM's
     * default cacerts trust check (PKIX path building failed) even though
     * curl and the browser reach it fine — a TLS-inspecting proxy/AV in
     * front of this machine injects a root CA that Windows trusts but the
     * JDK's bundled trust store does not (the exact Java-side counterpart of
     * the pip-system-certs fix this project already needed on the Python
     * side). Loading the OS "Windows-ROOT" store directly sidesteps that
     * without touching the JVM-wide default trust manager. Falls back to the
     * JVM default silently on any non-Windows host or load failure.
     */
    private HttpClient buildOutboundHttpsClient() {
        HttpClient.Builder builder = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(timeoutSeconds));

        if (IS_WINDOWS) {
            try {
                KeyStore windowsRoot = KeyStore.getInstance("Windows-ROOT");
                windowsRoot.load(null, null);
                TrustManagerFactory trustManagerFactory =
                        TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                trustManagerFactory.init(windowsRoot);
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, trustManagerFactory.getTrustManagers(), null);
                builder.sslContext(sslContext);
            } catch (Exception ignored) {
                // Fall back to the JVM's bundled cacerts trust store.
            }
        }

        return builder.build();
    }

    private String callOllama(String question, List<String> roles) {
        String systemPrompt = buildSystemPrompt(roles);

        Map<String, Object> payload = new HashMap<>();
        payload.put("model", ollamaModel);
        payload.put("messages", List.of(
            Map.of("role", "system", "content", systemPrompt),
            Map.of("role", "user", "content", question)
        ));
        payload.put("stream", false);
        payload.put("options", Map.of(
                "temperature", 0.2,
                "num_predict", 350
        ));

        try {
            String requestBody = objectMapper.writeValueAsString(payload);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ollamaApiUrl))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(timeoutSeconds))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                throw new IllegalStateException("Ollama request failed with status " + response.statusCode());
            }

            JsonNode root = objectMapper.readTree(response.body());
            String content = root.path("message").path("content").asText("").trim();
            if (content.isEmpty()) {
                throw new IllegalStateException("Empty response from Ollama");
            }

            return content;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to serialize or parse Ollama payload", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Ollama request was interrupted", e);
        }
    }

    private String buildSystemPrompt(List<String> roles) {
        String normalizedRoles = roles.stream()
                .map(r -> r.toUpperCase(Locale.ROOT))
                .collect(Collectors.joining(", "));

        return "You are an AI assistant for a multi-module platform. "
                + "Rules: "
                + "1) Only answer based on the user's role. "
                + "2) Do NOT expose sensitive data such as passwords, secrets, API keys, tokens, private keys, or credentials. "
                + "3) If user asks something forbidden, answer exactly: NOT AUTHORIZED. "
            + "4) Be concise, direct, and professional. "
            + "5) Do not provide hypothetical examples or generic filler lists. "
            + "6) If data is unavailable, say: No data available. "
                + "Current user roles: [" + normalizedRoles + "]. "
                + "If uncertain, answer NOT AUTHORIZED.";
    }

    private boolean isLowStockQuestion(String question) {
        String q = normalizeText(question);
        if (q.isBlank()) {
            return false;
        }

        if (q.contains("low stock")) {
            return true;
        }

        if (q.contains("out of stock")) {
            return true;
        }

        boolean mentionsStock = q.contains("stock") || q.contains("inventory") || q.contains("part");
        boolean mentionsLowLevel = q.contains("low") || q.contains("minimum") || q.contains("below");
        return mentionsStock && mentionsLowLevel;
    }

    private String buildLowStockDirectAnswer() {
        List<LowStockAlertResponse> lowStockItems = inventoryAnalyticsService.getLowStockAlerts();
        if (lowStockItems == null || lowStockItems.isEmpty()) {
            return "No low-stock items found.";
        }

        List<String> lines = new ArrayList<>();
        lines.add("Low-stock items:");

        for (LowStockAlertResponse item : lowStockItems) {
            String name = item.getPartName() == null || item.getPartName().isBlank() ? "Unknown part" : item.getPartName();
            Integer current = item.getCurrentStock();
            Integer minimum = item.getMinimumStock();
            Long partId = item.getPartId();

            lines.add(String.format(
                    "- %s (ID: %s): current=%s, minimum=%s",
                    name,
                    partId == null ? "N/A" : partId,
                    current == null ? "N/A" : current,
                    minimum == null ? "N/A" : minimum
            ));
        }

        return String.join("\n", lines);
    }

    private boolean isUsersByRoleQuestion(String question) {
        String q = normalizeText(question);
        if (q.isBlank()) {
            return false;
        }

        boolean mentionsUsers = q.contains("user") || q.contains("users") || q.contains("account") || q.contains("accounts");
        if (!mentionsUsers) {
            return false;
        }

        if (q.contains("role")) {
            return true;
        }

        return extractRequestedRole(q) != null;
    }

    private String buildUsersByRoleDirectAnswer(String question) {
        String requestedRole = extractRequestedRole(normalizeText(question));
        if (requestedRole == null) {
            return "Please specify a role (for example: ADMIN, MANAGER, TECHNICIAN).";
        }

        List<User> users = userRepository.findAll().stream()
                .filter(user -> user != null && user.getRoles() != null)
                .filter(user -> user.getRoles().stream().anyMatch(role -> {
                    if (role == null || role.getName() == null) {
                        return false;
                    }

                    String name = role.getName().toUpperCase(Locale.ROOT);
                    return name.equals(requestedRole) || name.equals("ROLE_" + requestedRole);
                }))
                .toList();

        if (users == null || users.isEmpty()) {
            return "No users found with role " + requestedRole + ".";
        }

        if (isCountQuestion(question)) {
            return "There are " + users.size() + " users with role " + requestedRole + ".";
        }

        List<String> usernames = users.stream()
                .map(User::getUsername)
                .filter(name -> name != null && !name.isBlank())
                .distinct()
                .sorted()
                .toList();

        if (usernames.isEmpty()) {
            return "Users with role " + requestedRole + ": " + users.size() + " found.";
        }

        return "Users with role " + requestedRole + " (" + usernames.size() + "): " + String.join(", ", usernames) + ".";
    }

    private static final List<MaintenanceStatus> ACTIVE_TASK_STATUSES =
            List.of(MaintenanceStatus.SCHEDULED, MaintenanceStatus.APPROVED, MaintenanceStatus.IN_PROGRESS);
    private static final DateTimeFormatter EVENT_TIME_FORMAT = DateTimeFormatter.ofPattern("EEE d MMM, HH:mm");

    /** First-person phrasing — "my tasks", "assigned to me", "what do I have", etc. */
    private boolean isPersonalScopeQuestion(String question) {
        String q = normalizeText(question);
        if (q.isBlank()) {
            return false;
        }
        return q.contains(" my ") || q.startsWith("my ") || q.contains(" me ") || q.endsWith(" me")
                || q.contains(" me?") || q.contains("i have") || q.contains("assigned to me")
                || q.contains("do i have");
    }

    /**
     * Answers "my tasks" / "my alerts" / "my events" (or "what's coming up",
     * "what's assigned to me") using the CALLING user's own identity —
     * resolved from the JWT, not from anything the question text supplies —
     * so one user can never see another's task list by asking about them in
     * the first person. Returns null (falls through to the generic
     * platform-wide shortcuts) if the question doesn't match a known
     * personal-scope intent or the caller can't be resolved to a User row.
     */
    private String buildPersonalDirectAnswer(String question, Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return null;
        }
        User caller = userRepository.findByUsername(authentication.getName()).orElse(null);
        if (caller == null) {
            return null;
        }

        String q = normalizeText(question);
        boolean asksTasks = q.contains("task");
        boolean asksAlerts = q.contains("alert");
        boolean asksEvents = q.contains("event") || q.contains("calendar") || q.contains("schedule")
                || q.contains("coming up") || q.contains("upcoming");

        if (asksTasks) {
            return myTasksAnswer(caller);
        }
        if (asksAlerts) {
            return myAlertsAnswer(caller);
        }
        if (asksEvents) {
            return myEventsAnswer(caller);
        }
        return null;
    }

    private String myTasksAnswer(User caller) {
        List<Maintenance> activeTasks = maintenanceRepository
                .findByAssignedTechnicianIdAndStatusIn(caller.getId(), ACTIVE_TASK_STATUSES);

        if (activeTasks.isEmpty()) {
            return "You have no active maintenance tasks assigned right now.";
        }

        List<String> lines = new ArrayList<>();
        lines.add("You have " + activeTasks.size() + " active task" + (activeTasks.size() == 1 ? "" : "s") + ":");
        activeTasks.stream().limit(5).forEach(task -> lines.add(String.format(
                "- [%s/%s] Machine %s: %s",
                task.getStatus(), task.getPriority(), task.getMachineId(),
                task.getDescription() == null ? "" : task.getDescription()
        )));
        if (activeTasks.size() > 5) {
            lines.add("...and " + (activeTasks.size() - 5) + " more.");
        }
        return String.join("\n", lines);
    }

    private String myAlertsAnswer(User caller) {
        List<Alert> myAlerts = alertRepository
                .findByAssignedTo(caller.getUsername(), PageRequest.of(0, 50))
                .getContent();

        long open = myAlerts.stream()
                .filter(a -> a.getStatus() == AlertStatus.NEW || a.getStatus() == AlertStatus.ACKNOWLEDGED
                        || a.getStatus() == AlertStatus.ESCALATED)
                .count();

        if (myAlerts.isEmpty()) {
            return "You have no alerts assigned to you.";
        }

        List<String> lines = new ArrayList<>();
        lines.add("You have " + open + " open alert" + (open == 1 ? "" : "s") + " (" + myAlerts.size() + " total assigned):");
        myAlerts.stream()
                .filter(a -> a.getStatus() == AlertStatus.NEW || a.getStatus() == AlertStatus.ACKNOWLEDGED
                        || a.getStatus() == AlertStatus.ESCALATED)
                .limit(5)
                .forEach(a -> lines.add(String.format("- [%s/%s] Machine %s: %s", a.getStatus(), a.getSeverity(), a.getMachineId(), a.getTitle())));
        return String.join("\n", lines);
    }

    private String myEventsAnswer(User caller) {
        LocalDateTime now = LocalDateTime.now();
        List<CalendarEventResponse> upcoming = calendarEventService
                .getEventsByTechnicianAndDateRange(caller.getUsername(), now, now.plusDays(7));

        if (upcoming.isEmpty()) {
            return "You have nothing scheduled in the next 7 days.";
        }

        List<String> lines = new ArrayList<>();
        lines.add("You have " + upcoming.size() + " event" + (upcoming.size() == 1 ? "" : "s") + " in the next 7 days:");
        upcoming.stream().limit(5).forEach(e -> lines.add(String.format(
                "- %s: %s", e.getStartTime().format(EVENT_TIME_FORMAT), e.getTitle()
        )));
        if (upcoming.size() > 5) {
            lines.add("...and " + (upcoming.size() - 5) + " more.");
        }
        return String.join("\n", lines);
    }

    private String buildPlatformDirectAnswer(String question) {
        if (question == null || question.isBlank()) {
            return null;
        }

        if (isUsersByRoleQuestion(question)) {
            return buildUsersByRoleDirectAnswer(question);
        }

        if (isCountQuestion(question)) {
            String countAnswer = buildCountDirectAnswer(question);
            if (countAnswer != null) {
                return countAnswer;
            }
        }

        String q = normalizeText(question);
        if (q.contains("platform summary")
                || q.contains("platform overview")
                || q.contains("system summary")
                || q.contains("dashboard summary")
                || q.contains("global overview")) {
            return buildPlatformSummary();
        }

        return null;
    }

    private boolean isCountQuestion(String question) {
        String q = normalizeText(question);
        if (q.isBlank()) {
            return false;
        }

        return q.contains("how many") || q.contains("count") || q.contains("number of");
    }

    private String buildCountDirectAnswer(String question) {
        String q = normalizeText(question);
        if (q.isBlank()) {
            return null;
        }

        String requestedRole = extractRequestedRole(q);
        boolean userRelated = q.contains("user") || q.contains("users") || q.contains("account") || q.contains("accounts");
        if (requestedRole != null && userRelated) {
            long count = userRepository.findAll().stream()
                    .filter(user -> user != null && user.getRoles() != null)
                    .filter(user -> user.getRoles().stream().anyMatch(role -> {
                        if (role == null || role.getName() == null) {
                            return false;
                        }
                        String name = role.getName().toUpperCase(Locale.ROOT);
                        return name.equals(requestedRole) || name.equals("ROLE_" + requestedRole);
                    }))
                    .count();
            return "There are " + count + " users with role " + requestedRole + ".";
        }

        if (q.contains("how many users") || q.contains("number of users") || q.equals("users count") || q.equals("user count")) {
            return "There are " + userRepository.count() + " users.";
        }

        if (q.contains("how many roles") || q.contains("number of roles") || q.equals("roles count") || q.equals("role count")) {
            long distinctRoles = userRepository.findAll().stream()
                    .filter(user -> user != null && user.getRoles() != null)
                    .flatMap(user -> user.getRoles().stream())
                    .filter(role -> role != null && role.getName() != null)
                    .map(role -> role.getName().toUpperCase(Locale.ROOT))
                    .distinct()
                    .count();
            return "There are " + distinctRoles + " roles assigned to users.";
        }

        if (q.contains("how many machines") || q.contains("number of machines") || q.equals("machine count") || q.equals("machines count")) {
            return "There are " + machineRepository.count() + " machines.";
        }

        if (q.contains("how many mainten") || q.contains("number of mainten") || q.contains("maintenance count")) {
            return "There are " + maintenanceRepository.count() + " maintenance records.";
        }

        if (q.contains("how many scheduled maintenance") || q.contains("scheduled maintenance count")) {
            long count = maintenanceRepository.findByStatus(MaintenanceStatus.SCHEDULED, PageRequest.of(0, 1)).getTotalElements();
            return "There are " + count + " scheduled maintenance records.";
        }

        if (q.contains("how many completed maintenance") || q.contains("completed maintenance count")) {
            long count = maintenanceRepository.findByStatus(MaintenanceStatus.COMPLETED, PageRequest.of(0, 1)).getTotalElements();
            return "There are " + count + " completed maintenance records.";
        }

        if (q.contains("how many in progress maintenance") || q.contains("maintenance in progress")) {
            long count = maintenanceRepository.findByStatus(MaintenanceStatus.IN_PROGRESS, PageRequest.of(0, 1)).getTotalElements();
            return "There are " + count + " maintenance records in progress.";
        }

        if (q.contains("how many alerts") || q.contains("number of alerts") || q.equals("alerts count") || q.equals("alert count")) {
            return "There are " + alertRepository.count() + " alerts.";
        }

        if (q.contains("unresolved alerts") || q.contains("open alerts")) {
            return "There are " + alertRepository.countUnresolvedAlerts() + " unresolved alerts.";
        }

        if (q.contains("new alerts") || q.contains("unacknowledged alerts")) {
            return "There are " + alertRepository.countByStatus(AlertStatus.NEW) + " new alerts.";
        }

        if (q.contains("escalated alerts")) {
            return "There are " + alertRepository.countByStatus(AlertStatus.ESCALATED) + " escalated alerts.";
        }

        if (q.contains("closed alerts")) {
            return "There are " + alertRepository.countByStatus(AlertStatus.CLOSED) + " closed alerts.";
        }

        boolean inventoryWords = q.contains("inventory") || q.contains("stock") || q.contains("items") || q.contains("parts");
        if (inventoryWords && (q.contains("how many") || q.contains("number of") || q.contains("count"))) {
            if (q.contains("low stock")) {
                return "There are " + partRepository.countByStatus(PartStatus.LOW_STOCK) + " low-stock items.";
            }

            if (q.contains("out of stock")) {
                return "There are " + partRepository.countByStatus(PartStatus.OUT_OF_STOCK) + " out-of-stock items.";
            }

            if (q.contains("pending reorder") || q.contains("reorder pending")) {
                return "There are " + inventoryAnalyticsService.countPendingReorderApprovals() + " pending reorder requests.";
            }

            if (q.contains("pending order") || q.contains("open order")) {
                return "There are " + inventoryAnalyticsService.countPendingOrders() + " pending stock orders.";
            }

            return "There are " + partRepository.count() + " inventory items.";
        }

        // Last resort: "how many technicians/managers/etc are there" names a
        // role but not the literal word "user" — every more specific branch
        // above (machines/maintenance/alerts/inventory) had first refusal and
        // found nothing, so a bare role name here can only mean a headcount.
        if (requestedRole != null) {
            long count = userRepository.findAll().stream()
                    .filter(user -> user != null && user.getRoles() != null)
                    .filter(user -> user.getRoles().stream().anyMatch(role -> {
                        if (role == null || role.getName() == null) {
                            return false;
                        }
                        String name = role.getName().toUpperCase(Locale.ROOT);
                        return name.equals(requestedRole) || name.equals("ROLE_" + requestedRole);
                    }))
                    .count();
            return "There are " + count + " users with role " + requestedRole + ".";
        }

        return null;
    }

    private String buildPlatformSummary() {
        long totalUsers = userRepository.count();
        long totalMachines = machineRepository.count();
        long totalInventoryItems = partRepository.count();
        long lowStockItems = partRepository.countByStatus(PartStatus.LOW_STOCK);
        long outOfStockItems = partRepository.countByStatus(PartStatus.OUT_OF_STOCK);
        long pendingReorders = inventoryAnalyticsService.countPendingReorderApprovals();
        long pendingOrders = inventoryAnalyticsService.countPendingOrders();
        long totalAlerts = alertRepository.count();
        long unresolvedAlerts = alertRepository.countUnresolvedAlerts();
        long totalMaintenance = maintenanceRepository.count();

        return String.join("\n",
                "Platform summary:",
                "- Users: " + totalUsers,
                "- Machines: " + totalMachines,
                "- Inventory items: " + totalInventoryItems,
                "- Low-stock items: " + lowStockItems,
                "- Out-of-stock items: " + outOfStockItems,
                "- Pending reorder requests: " + pendingReorders,
                "- Pending stock orders: " + pendingOrders,
                "- Alerts: " + totalAlerts,
                "- Unresolved alerts: " + unresolvedAlerts,
                "- Maintenance records: " + totalMaintenance
        );
    }

    private String extractRequestedRole(String normalizedQuestion) {
        if (normalizedQuestion == null || normalizedQuestion.isBlank()) {
            return null;
        }

        String q = normalizedQuestion.replace('-', ' ').replace('_', ' ');
        Set<String> candidates = new LinkedHashSet<>();

        if (q.contains("super admin")) {
            candidates.add("SUPER_ADMIN");
        }
        if (q.contains("stock manager")) {
            candidates.add("STOCK_MANAGER");
        }
        if (q.contains("data scientist")) {
            candidates.add("DATA_SCIENTIST");
        }

        for (String role : KNOWN_ROLES) {
            String roleWord = role.toLowerCase(Locale.ROOT).replace("_", " ");
            if (q.contains(roleWord)) {
                candidates.add(role);
            }
        }

        if (q.contains("admins") || q.contains("admin users")) {
            candidates.add("ADMIN");
        }

        return candidates.stream().findFirst().orElse(null);
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).trim();
    }

    private Set<String> extractRoles(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return Set.of();
        }

        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(this::normalizeRole)
                .collect(Collectors.toSet());
    }

    private String normalizeRole(String authority) {
        if (authority == null) {
            return "";
        }
        return authority.startsWith("ROLE_") ? authority.substring(5) : authority;
    }

    private String resolveApiKey() {
        if (openAiApiKey != null && !openAiApiKey.isBlank()) {
            return openAiApiKey.trim();
        }

        String envValue = System.getenv("OPENAI_API_KEY");
        if (envValue != null && !envValue.isBlank()) {
            return envValue.trim();
        }

        String fromLocalDotEnv = readKeyFromDotEnv(Path.of(".env"));
        if (fromLocalDotEnv != null && !fromLocalDotEnv.isBlank()) {
            return fromLocalDotEnv;
        }

        String fromParentDotEnv = readKeyFromDotEnv(Path.of("..", ".env"));
        if (fromParentDotEnv != null && !fromParentDotEnv.isBlank()) {
            return fromParentDotEnv;
        }

        return null;
    }

    private String readKeyFromDotEnv(Path filePath) {
        try {
            if (!Files.exists(filePath)) {
                return null;
            }

            List<String> lines = Files.readAllLines(filePath);
            for (String line : lines) {
                if (line == null) {
                    continue;
                }

                String trimmed = line.trim();
                if (!trimmed.startsWith("OPENAI_API_KEY=")) {
                    continue;
                }

                String value = trimmed.substring("OPENAI_API_KEY=".length()).trim();
                return value.isBlank() ? null : value;
            }
        } catch (IOException ignored) {
            return null;
        }

        return null;
    }
}
