package com.pfe.predictive.config;

import com.pfe.predictive.core.entity.User;
import com.pfe.predictive.data.repository.RoleRepository;
import com.pfe.predictive.data.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * User Seed Initializer
 * Seeds default users for testing and initial setup
 * 
 * Creates:
 * - Super Admin user (superadmin/superadmin)
 * - Technician user (technician/technician)
 * 
 * @author Predictive Maintenance System
 * @version 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserSeedInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public void run(String... args) {
        // Skip if users already exist
        if (userRepository.count() > 0) {
            log.info("Users already initialized - skipping");
            return;
        }

        log.info("Initializing default users...");

        seedUser(
                "superadmin",
                "superadmin@predictive-maintenance.local",
                "superadmin",
                "Super",
                "Administrator",
                "Super Admin",
                "IT",
                List.of("SUPER_ADMIN", "ADMIN")
        );

        seedUser(
                "technician",
                "technician@predictive-maintenance.local",
                "technician",
                "Default",
                "Technician",
                "Maintenance Tech",
                "Operations",
                List.of("TECHNICIAN")
        );

        log.info("Default users initialized successfully!");
    }

    private void seedUser(String username,
                          String email,
                          String rawPassword,
                          String firstName,
                          String lastName,
                          String displayName,
                          String department,
                          List<String> roleNames) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setDisplayName(displayName);
        user.setDepartment(department);
        user.setStatus("ACTIVE");
        user.setLocked(false);
        user.setMfaEnabled(false);

        for (String roleName : roleNames) {
            roleRepository.findByName(roleName).ifPresent(user::addRole);
        }

        userRepository.save(user);
        log.info("Created user: {} with roles: {}", username, roleNames);
    }
}
