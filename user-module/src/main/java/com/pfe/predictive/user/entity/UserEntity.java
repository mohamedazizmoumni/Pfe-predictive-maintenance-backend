package com.pfe.predictive.user.entity;

import lombok.*;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(length = 255)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role; // TECHNICIAN, MANAGER, STOCK_MANAGER, DATA_SCIENTIST, ADMIN, SUPER_ADMIN

    @Enumerated(EnumType.STRING)
    @Column
    private UserStatus status; // ACTIVE, INACTIVE, LOCKED

    @Column
    private LocalDateTime lastLoginDate;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @UpdateTimestamp
    @Column
    private LocalDateTime lastModifiedDate;

    @Version
    private Long version;
}

public enum UserRole {
    TECHNICIAN,
    MANAGER,
    STOCK_MANAGER,
    DATA_SCIENTIST,
    ADMIN,
    SUPER_ADMIN
}

public enum UserStatus {
    ACTIVE,
    INACTIVE,
    LOCKED
}
