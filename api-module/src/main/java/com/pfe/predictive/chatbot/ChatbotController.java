package com.pfe.predictive.chatbot;

import com.pfe.predictive.chatbot.dto.ChatbotRequest;
import com.pfe.predictive.chatbot.dto.ChatbotResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/chatbot")
public class ChatbotController {

    private final ChatbotService chatbotService;

    public ChatbotController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    @PostMapping("/ask")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> ask(@Valid @RequestBody ChatbotRequest request, Authentication authentication) {
        try {
            ChatbotResponse response = chatbotService.askQuestion(request.getQuestion(), authentication);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            String message = e.getMessage() == null ? "Chatbot service unavailable" : e.getMessage();

            if (message.contains("status 429")) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(Map.of("error", "OpenAI quota exceeded. Please check billing/quota and retry."));
            }

            if (message.contains("status 401")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "OpenAI API key is invalid or revoked."));
            }

            if (message.contains("status 403")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "OpenAI access is forbidden for the configured key/project."));
            }

            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", message));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to process chatbot request"));
        }
    }
}
