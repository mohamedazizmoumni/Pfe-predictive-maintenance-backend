package com.pfe.predictive.data.repository;

import com.pfe.predictive.core.entity.UserPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserPreferenceRepository extends JpaRepository<UserPreference, Long> {
    Optional<UserPreference> findByUserIdAndPrefKey(Long userId, String prefKey);
    List<UserPreference> findByUserId(Long userId);
}
