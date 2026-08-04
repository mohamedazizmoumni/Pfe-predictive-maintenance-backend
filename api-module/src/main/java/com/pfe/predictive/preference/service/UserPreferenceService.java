package com.pfe.predictive.preference.service;

import com.pfe.predictive.core.entity.User;
import com.pfe.predictive.core.entity.UserPreference;
import com.pfe.predictive.data.repository.UserPreferenceRepository;
import com.pfe.predictive.data.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class UserPreferenceService {

    private final UserPreferenceRepository preferenceRepository;
    private final UserRepository userRepository;

    public Long resolveUserId(String username) {
        return userRepository.findByUsername(username)
                .map(User::getId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + username));
    }

    @Transactional(readOnly = true)
    public Map<String, String> getAll(Long userId) {
        return preferenceRepository.findByUserId(userId).stream()
                .collect(Collectors.toMap(UserPreference::getPrefKey, UserPreference::getPrefValue));
    }

    @Transactional(readOnly = true)
    public String get(Long userId, String key, String defaultValue) {
        return preferenceRepository.findByUserIdAndPrefKey(userId, key)
                .map(UserPreference::getPrefValue)
                .orElse(defaultValue);
    }

    public void set(Long userId, String key, String value) {
        UserPreference preference = preferenceRepository.findByUserIdAndPrefKey(userId, key)
                .orElseGet(() -> UserPreference.builder().userId(userId).prefKey(key).build());
        preference.setPrefValue(value);
        preferenceRepository.save(preference);
    }
}
