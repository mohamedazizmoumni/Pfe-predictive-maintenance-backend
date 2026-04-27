package com.pfe.predictive.config;

import com.pfe.predictive.config.provider.JwtTokenProvider;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        System.out.println("🔥 JWT FILTER HIT: " + request.getRequestURI());

        String header = request.getHeader("Authorization");

        try {
            if (header != null && header.startsWith("Bearer ")) {

                String token = header.substring(7);

                jwtTokenProvider.validateAndGetClaims(token);

                String username = jwtTokenProvider.getUsernameFromToken(token);
                List<String> roles = jwtTokenProvider.getRolesFromToken(token);

                if (roles == null) roles = List.of();

                var authorities = roles.stream()
                        .map(role -> {
                            if (role == null) {
                                return null;
                            }
                            String cleanRole = role.trim()
                                    .replace("[", "")
                                    .replace("]", "")
                                    .replace("\"", "");
                            if (cleanRole.startsWith("ROLE_")) {
                                cleanRole = cleanRole.substring(5);
                            }
                            if (cleanRole.isBlank()) {
                                return null;
                            }
                            return new SimpleGrantedAuthority("ROLE_" + cleanRole);
                        })
                        .filter(Objects::nonNull)
                        .toList();

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(username, null, authorities);

                SecurityContextHolder.getContext().setAuthentication(auth);

                System.out.println("✅ AUTH SET: " + auth);
            }

        } catch (JwtException e) {
            System.out.println("❌ JWT ERROR: " + e.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}