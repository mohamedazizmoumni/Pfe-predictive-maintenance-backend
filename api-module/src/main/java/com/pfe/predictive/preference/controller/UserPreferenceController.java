package com.pfe.predictive.preference.controller;

import com.pfe.predictive.preference.dto.UserPreferenceValue;
import com.pfe.predictive.preference.service.UserPreferenceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Self-scoped preference store — every call operates on the caller's own
 * preferences, resolved from the JWT, never a userId in the URL/body.
 */
@RestController
@RequestMapping("/api/v1/preferences")
@RequiredArgsConstructor
public class UserPreferenceController {

    private final UserPreferenceService preferenceService;

    @GetMapping
    public ResponseEntity<Map<String, String>> getAll(Authentication authentication) {
        Long userId = preferenceService.resolveUserId(authentication.getName());
        return ResponseEntity.ok(preferenceService.getAll(userId));
    }

    @GetMapping("/{key}")
    public ResponseEntity<String> get(@PathVariable String key, Authentication authentication) {
        Long userId = preferenceService.resolveUserId(authentication.getName());
        return ResponseEntity.ok(preferenceService.get(userId, key, ""));
    }

    @PutMapping("/{key}")
    public ResponseEntity<Void> set(@PathVariable String key, @Valid @RequestBody UserPreferenceValue body,
                                     Authentication authentication) {
        Long userId = preferenceService.resolveUserId(authentication.getName());
        preferenceService.set(userId, key, body.getValue());
        return ResponseEntity.noContent().build();
    }
}
