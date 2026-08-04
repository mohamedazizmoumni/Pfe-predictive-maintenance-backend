package com.pfe.predictive.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.web.cors.*;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${cors.allowed-origins:http://localhost:4200}")
    private List<String> allowedOrigins;

    @Value("${cors.allowed-methods:GET,POST,PUT,DELETE,OPTIONS,PATCH}")
    private List<String> allowedMethods;

    @Value("${cors.allowed-headers:*}")
    private List<String> allowedHeaders;

    @Value("${cors.exposed-headers:Authorization,Content-Type}")
    private List<String> exposedHeaders;

    @Value("${cors.allow-credentials:true}")
    private boolean allowCredentials;

    @Value("${cors.max-age:3600}")
    private long maxAge;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(allowedMethods);
        config.setAllowedHeaders(allowedHeaders);
        config.setExposedHeaders(exposedHeaders);
        config.setAllowCredentials(allowCredentials);
        config.setMaxAge(maxAge);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }

    @Bean
    @Order(1)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())

            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Return 401 for unauthenticated calls (missing/expired/invalid JWT)
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))

            // 🔥 ORDER IS CRITICAL
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

            .authorizeHttpRequests(auth -> auth

                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/api/v1/public/**").permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/error").permitAll()

                .requestMatchers("/health").permitAll()
                .requestMatchers("/actuator/**")
                    .hasAnyAuthority("ROLE_ADMIN", "ROLE_SUPER_ADMIN")

                // Profile picture GET is public so <img> tags work without JWT
                .requestMatchers(HttpMethod.GET, "/api/v1/profile/*/picture").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/profile/*/picture/exists").permitAll()

                // Uploaded static files (part images, profile pictures) are served via
                // StaticResourceConfig and rendered through plain <img> tags, which don't
                // send the Authorization header — must be public GET, same reasoning as above.
                .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()

                // ML service posts inventory shortage predictions here. No JWT from an
                // external ML caller, so this bypasses JWT auth and is instead validated
                // via the X-Internal-Key header inside InventoryController, the same
                // shared-secret trust model already used for outbound Java->ML calls
                // (see MlRestTemplateConfig).
                .requestMatchers(HttpMethod.POST, "/api/v1/inventory/parts/*/shortage-forecast").permitAll()

                // User management: read is open to all authenticated users;
                // write (create / update / delete / face-reset) is admin-only
                // (enforced at the controller level via @PreAuthorize — kept
                // here as a defence-in-depth layer).
                //
                // Profile-picture sub-resource is the exception: any authenticated
                // user may manage their own picture (self-or-admin check is
                // enforced in UserProfilePictureController's @PreAuthorize), so
                // these narrower matchers must come before the general /users/**
                // admin-only rules below (Spring Security uses the first match).
                .requestMatchers(HttpMethod.GET, "/api/v1/users/**").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/v1/users/*/profile-picture").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/v1/users/*/profile-picture").authenticated()
                .requestMatchers(HttpMethod.POST,   "/api/v1/users/**")
                    .hasAnyAuthority("ROLE_ADMIN", "ROLE_SUPER_ADMIN")
                // PUT (update) is the exception: any authenticated user may edit
                // their own profile (name/email/phone/department), same self-or-admin
                // pattern as the profile-picture matchers above — the real ownership
                // check (self vs. admin, and which fields a non-admin may touch) lives
                // in UserController.updateUser's @PreAuthorize + in-method check.
                .requestMatchers(HttpMethod.PUT,    "/api/v1/users/**").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/v1/users/**")
                    .hasAnyAuthority("ROLE_ADMIN", "ROLE_SUPER_ADMIN")
                
                // WebSocket endpoints - allow for handshake
                .requestMatchers("/ws-machine/**").permitAll()
                .requestMatchers("/ws-nlp/**").permitAll()
                .requestMatchers("/ws-alerts/**").permitAll()
                .requestMatchers("/topic/**").permitAll()
                .requestMatchers("/app/**").permitAll()

                // Trigger endpoints force ML calls, DB writes, and alert/email dispatch —
                // admin-only. Info/health are read-only and just need a valid session.
                .requestMatchers(HttpMethod.POST, "/api/v1/streaming/trigger", "/api/v1/streaming/trigger/**")
                    .hasAnyAuthority("ROLE_ADMIN", "ROLE_SUPER_ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/v1/streaming/**").authenticated()

                .requestMatchers("/api/nlp/**")
                    .hasAnyAuthority("ROLE_TECHNICIAN", "ROLE_MANAGER", "ROLE_DATA_SCIENTIST", "ROLE_ADMIN", "ROLE_SUPER_ADMIN")

                .requestMatchers("/api/v1/nlp/**")
                    .hasAnyAuthority("ROLE_TECHNICIAN", "ROLE_MANAGER", "ROLE_DATA_SCIENTIST", "ROLE_ADMIN", "ROLE_SUPER_ADMIN")

                .requestMatchers("/api/v1/maintenance/**")
                    .hasAnyAuthority("ROLE_TECHNICIAN", "ROLE_MANAGER", "ROLE_ADMIN", "ROLE_SUPER_ADMIN")

                .requestMatchers("/api/v1/dashboard/**")
                    .hasAnyAuthority("ROLE_TECHNICIAN", "ROLE_MANAGER", "ROLE_STOCK_MANAGER", "ROLE_DATA_SCIENTIST", "ROLE_ADMIN", "ROLE_SUPER_ADMIN")

                .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()

                .anyRequest().authenticated()
            );

        return http.build();
    }
}