package com.pfe.predictive.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import java.util.UUID;

/**
 * Narrow filter chain for Prometheus scraping only. /actuator/prometheus is
 * otherwise ADMIN-role-gated behind the JWT flow (see SecurityConfig), but a
 * JWT expires every jwt.expiration ms with no way for a static Prometheus
 * scrape_config to refresh it — a long-lived HTTP Basic credential is the
 * standard pattern for scrapers instead (docs/monitoring.md in sentinel-devops
 * calls this out as the pre-production TODO; this is that fix).
 *
 * <p>@Order(0) makes this chain's securityMatcher claim /actuator/prometheus
 * before SecurityConfig's general chain (@Order(1)) ever evaluates the
 * request — every other /actuator/** path is untouched by this class.
 */
@Configuration
public class PrometheusScrapeSecurityConfig {

    @Value("${monitoring.prometheus.username:prometheus}")
    private String scrapeUsername;

    // No usable default on purpose: an empty configured password would let
    // anyone authenticate with username=prometheus, password="". Falling
    // back to a random UUID instead means scraping is simply refused until
    // MONITORING_PROMETHEUS_PASSWORD is actually set — same safe-by-default
    // posture as JwtTokenProvider's missing-secret guard.
    @Value("${monitoring.prometheus.password:}")
    private String configuredPassword;

    @Bean
    public UserDetailsService prometheusScrapeUser(PasswordEncoder passwordEncoder) {
        String password = (configuredPassword == null || configuredPassword.isBlank())
                ? UUID.randomUUID().toString()
                : configuredPassword;

        return new InMemoryUserDetailsManager(
                User.withUsername(scrapeUsername)
                        .password(passwordEncoder.encode(password))
                        .roles("MONITORING")
                        .build());
    }

    @Bean
    @Order(0)
    public SecurityFilterChain prometheusScrapeFilterChain(HttpSecurity http,
                                                             UserDetailsService prometheusScrapeUser) throws Exception {
        http.securityMatcher("/actuator/prometheus")
                .userDetailsService(prometheusScrapeUser)
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(org.springframework.security.config.http.SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().hasRole("MONITORING"))
                .httpBasic(basic -> {});

        return http.build();
    }
}
