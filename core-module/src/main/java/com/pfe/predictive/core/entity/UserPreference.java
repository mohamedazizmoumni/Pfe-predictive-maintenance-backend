package com.pfe.predictive.core.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Generic per-user key/value store — one row per (userId, prefKey). Reused
 * for notification preferences ("notifications") and dashboard widget
 * layout ("dashboardLayout:<dashboard>"). Value is a plain string; callers
 * agree on JSON where structure is needed, mirroring how CalendarEvent
 * stores free-form text elsewhere in this codebase.
 */
@Entity
@Table(name = "user_preferences", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "pref_key"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "pref_key", nullable = false, length = 100)
    private String prefKey;

    @Column(name = "pref_value", nullable = false, length = 4000)
    private String prefValue;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @UpdateTimestamp
    private LocalDateTime lastModifiedDate;
}
