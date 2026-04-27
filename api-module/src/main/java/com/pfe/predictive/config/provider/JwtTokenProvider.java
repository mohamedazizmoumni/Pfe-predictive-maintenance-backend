package com.pfe.predictive.config.provider;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    public JwtTokenProvider(
            @Value("${jwt.secret:your-super-secret-jwt-key-change-this-in-production-at-least-256-bits-long-minimum-32-characters}") String jwtSecret,
            @Value("${jwt.expiration:3600000}") long accessExpiration,      // e.g. 60 minutes
            @Value("${jwt.refresh.expiration:604800000}") long refreshExpiration) { // e.g. 7 days

        if (jwtSecret == null || jwtSecret.length() < 32) {
            System.err.println("⚠️ WARNING: JWT secret is too short! Use at least 32 characters.");
        }

        this.secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiration = accessExpiration;
        this.refreshTokenExpiration = refreshExpiration;

        System.out.println("✅ JWT Provider initialized:");
        System.out.println("   Access Token Expiration : " + accessExpiration + "ms (" + (accessExpiration / 1000 / 60) + " minutes)");
        System.out.println("   Refresh Token Expiration: " + refreshExpiration + "ms (" + (refreshExpiration / 1000 / 3600) + " hours)");
    }

    // ==================== ACCESS TOKEN ====================
    public String generateAccessToken(String username, String email, List<String> roles) {
        long now = System.currentTimeMillis();
        Date issuedAt = new Date(now);
        Date expiryDate = new Date(now + accessTokenExpiration);

        System.out.println("🔐 Generating ACCESS token for user: " + username);

        return Jwts.builder()
                .setSubject(username)
                .claim("email", email)
                .claim("roles", roles)
                .setIssuedAt(issuedAt)
                .setExpiration(expiryDate)
                .signWith(secretKey, SignatureAlgorithm.HS512)
                .compact();
    }

    // ==================== REFRESH TOKEN ====================
    public String generateRefreshToken(String username) {
        long now = System.currentTimeMillis();
        Date issuedAt = new Date(now);
        Date expiryDate = new Date(now + refreshTokenExpiration);

        System.out.println("🔄 Generating REFRESH token for user: " + username 
                + " (expires in " + (refreshTokenExpiration / 1000 / 3600) + " hours)");

        return Jwts.builder()
                .setSubject(username)
                .claim("type", "refresh")           // Important marker
                .setIssuedAt(issuedAt)
                .setExpiration(expiryDate)
                .signWith(secretKey, SignatureAlgorithm.HS512)
                .compact();
    }

    // ==================== VALIDATION ====================
    public Claims validateAndGetClaims(String token) {
        try {
            if (token == null || token.trim().isEmpty()) {
                throw new JwtException("Token is empty or null");
            }

            Claims claims = Jwts.parser()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            System.out.println("✅ Token validated successfully for user: " + claims.getSubject());
            return claims;
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            System.err.println("❌ Token is EXPIRED: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            System.err.println("❌ Token validation failed: " + e.getMessage());
            throw new JwtException("Invalid token", e);
        }
    }

    public String getUsernameFromToken(String token) {
        return validateAndGetClaims(token).getSubject();
    }

    public String getEmailFromToken(String token) {
        return (String) validateAndGetClaims(token).get("email");
    }

    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        Claims claims = validateAndGetClaims(token);

        Object rolesClaim = claims.get("roles");
        if (rolesClaim instanceof List<?> roleList) {
            return roleList.stream()
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .toList();
        }

        if (rolesClaim instanceof String rolesString) {
            return Arrays.stream(rolesString.split(","))
                    .map(String::trim)
                    .map(s -> s.replace("[", "").replace("]", "").replace("\"", ""))
                    .filter(s -> !s.isBlank())
                    .toList();
        }

        Object roleClaim = claims.get("role");
        if (roleClaim instanceof String singleRole && !singleRole.isBlank()) {
            return List.of(singleRole.trim());
        }

        return Collections.emptyList();
    }

    public boolean isTokenExpired(String token) {
        try {
            Claims claims = validateAndGetClaims(token);
            return claims.getExpiration().before(new Date());
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            return true;
        } catch (Exception e) {
            return true;
        }
    }

    public void debugToken(String token) {
        try {
            Claims claims = validateAndGetClaims(token);
            System.out.println("🔍 Token Debug:");
            System.out.println("   Subject: " + claims.getSubject());
            System.out.println("   Type   : " + claims.get("type"));
            System.out.println("   Expires: " + claims.getExpiration());
        } catch (Exception e) {
            System.err.println("❌ Token debug failed: " + e.getMessage());
        }
    }
}