package com.yourpackage.business.config;

import com.pfe.predictive.core.entity.Role;
import com.pfe.predictive.core.entity.User;
import com.pfe.predictive.data.repository.RoleRepository;
import com.pfe.predictive.data.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UserSeedInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserSeedInitializer(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }

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
    }
}