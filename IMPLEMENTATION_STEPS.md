# Implementation Steps - Phase-by-Phase

## Overview

This document provides **concrete, step-by-step instructions** to implement the refactored architecture. Follow each phase sequentially.

---

## PHASE 1: Foundation Setup (Days 1-2)

### Step 1.1: Create Module Directories

```bash
# Navigate to your project
cd /path/to/Pfe-predictive-maintenance-backend

# Create module directories
mkdir -p common-module/src/main/java/com/pfe/predictive/{security,config,utils}
mkdir -p common-module/src/main/resources
mkdir -p api-module/src/main/java/com/pfe/predictive/dashboard/{controller,service,dto}

# Create feature module directories
for module in inventory maintenance task alert machine prediction user; do
  mkdir -p ${module}-module/src/main/java/com/pfe/predictive/${module}/{entity,repository,service,controller,dto,mapper}
  mkdir -p ${module}-module/src/main/resources
done
```

### Step 1.2: Add PermissionConstants.java

Already provided. Copy to:
```
common-module/src/main/java/com/pfe/predictive/security/PermissionConstants.java
```

### Step 1.3: Create SecurityConfig.java

```java
// File: common-module/src/main/java/com/pfe/predictive/config/SecurityConfig.java

package com.pfe.predictive.config;

import com.pfe.predictive.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserDetailsService userDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder authenticationManagerBuilder = 
            http.getSharedObject(AuthenticationManagerBuilder.class);
        authenticationManagerBuilder
            .userDetailsService(userDetailsService)
            .passwordEncoder(passwordEncoder());
        return authenticationManagerBuilder.build();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .cors()
            .and()
            .authorizeRequests()
                .antMatchers("/api/v1/auth/**", "/actuator/**").permitAll()
                .antMatchers("/api/v1/**").authenticated()
                .anyRequest().authenticated()
            .and()
            .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
```

### Step 1.4: Create JwtAuthenticationFilter.java

```java
// File: common-module/src/main/java/com/pfe/predictive/security/JwtAuthenticationFilter.java

package com.pfe.predictive.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * JWT Authentication Filter
 * 
 * Intercepts requests, extracts JWT token from Authorization header,
 * validates, and sets Spring Security context
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                   FilterChain filterChain) throws ServletException, IOException {
        try {
            String jwt = extractJwtFromRequest(request);
            
            if (jwt != null && jwtTokenProvider.validateToken(jwt)) {
                String username = jwtTokenProvider.getUsernameFromJWT(jwt);
                String role = jwtTokenProvider.getRoleFromJWT(jwt);
                
                List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                authorities.add(new SimpleGrantedAuthority(role));
                
                UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(username, null, authorities);
                
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("Set Spring Security context for user: {} with role: {}", username, role);
            }
        } catch (Exception ex) {
            log.error("JWT authentication failed", ex);
        }
        
        filterChain.doFilter(request, response);
    }

    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
```

### Step 1.5: Create JwtTokenProvider.java

```java
// File: common-module/src/main/java/com/pfe/predictive/security/JwtTokenProvider.java

package com.pfe.predictive.security;

import io.jsonwebtoken.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.Date;

@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret:MySecretKeyThatIsAtLeast32CharactersLongForHS256}")
    private String jwtSecret;

    @Value("${jwt.expiration:86400000}") // 24 hours
    private long jwtExpirationInMs;

    public String generateToken(String username, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);

        return Jwts.builder()
            .setSubject(username)
            .claim("role", role)
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .signWith(SignatureAlgorithm.HS512, jwtSecret)
            .compact();
    }

    public String getUsernameFromJWT(String token) {
        Claims claims = Jwts.parser()
            .setSigningKey(jwtSecret)
            .parseClaimsJws(token)
            .getBody();
        return claims.getSubject();
    }

    public String getRoleFromJWT(String token) {
        Claims claims = Jwts.parser()
            .setSigningKey(jwtSecret)
            .parseClaimsJws(token)
            .getBody();
        return (String) claims.get("role");
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .setSigningKey(jwtSecret)
                .parseClaimsJws(token);
            return true;
        } catch (MalformedJwtException | ExpiredJwtException | UnsupportedJwtException | 
                 IllegalArgumentException | SignatureException ex) {
            log.error("JWT validation failed", ex);
            return false;
        }
    }
}
```

### Step 1.6: Update pom.xml (Parent)

Add these dependencies to your parent `pom.xml`:

```xml
<!-- JWT dependencies -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt</artifactId>
    <version>0.11.5</version>
</dependency>

<!-- Lombok -->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <version>1.18.30</version>
    <scope>provided</scope>
</dependency>

<!-- Spring Security -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<!-- JPA: @CreationTimestamp, @UpdateTimestamp, @Version -->
<dependency>
    <groupId>org.hibernate.orm</groupId>
    <artifactId>hibernate-core</artifactId>
    <version>6.2.1.Final</version>
</dependency>
```

### Step 1.7: Create application.yml

```yaml
# File: /common-module/src/main/resources/application.yml

spring:
  application:
    name: predictive-maintenance-backend
  jpa:
    hibernate:
      ddl-auto: validate  # Flyway handles migrations
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQL10Dialect
        format_sql: true
        jdbc:
          batch_size: 20
          fetch_size: 50
  datasource:
    url: jdbc:postgresql://localhost:5432/predictive_maintenance
    username: postgres
    password: postgres
    driver-class-name: org.postgresql.Driver
  flyway:
    locations: classpath:db/migration
    placeholderReplacement: false

jwt:
  secret: MySecretKeyThatIsAtLeast32CharactersLongForHS256
  expiration: 86400000  # 24 hours

server:
  port: 8080
  servlet:
    context-path: /

logging:
  level:
    root: INFO
    com.pfe.predictive: DEBUG
```

### Step 1.8: Test Foundation

```bash
# Compile
mvn clean compile

# Run basic test
mvn spring-boot:run

# Should start successfully on port 8080
# Verify: curl http://localhost:8080/actuator/health
```

✅ **Phase 1 Complete**: Foundation (PermissionConstants, Security, Config, JWT)

---

## PHASE 2: Dashboard Implementation (Day 3)

### Step 2.1: Dashboard DTOs

Already provided. Copy to:
```
api-module/src/main/java/com/pfe/predictive/dashboard/dto/DashboardResponse.java
```

### Step 2.2: Dashboard Service

Already provided. Copy to:
```
api-module/src/main/java/com/pfe/predictive/dashboard/service/DashboardService.java
```

### Step 2.3: Dashboard Controller

Already provided. Copy to:
```
api-module/src/main/java/com/pfe/predictive/dashboard/controller/DashboardController.java
```

### Step 2.4: Create Query Service Stubs

You need to create stub implementations of query services. Dashboard requires:

```java
// maintenance-module/src/main/java/com/pfe/predictive/maintenance/service/MaintenanceQueryService.java
// inventory-module/src/main/java/com/pfe/predictive/inventory/service/InventoryAnalyticsService.java
// alert-module/src/main/java/com/pfe/predictive/alert/service/AlertQueryService.java
// task-module/src/main/java/com/pfe/predictive/task/service/TaskQueryService.java
// machine-module/src/main/java/com/pfe/predictive/machine/service/MachineQueryService.java
// prediction-module/src/main/java/com/pfe/predictive/prediction/service/PredictionAnalyticsService.java
```

Example for AlertQueryService:

```java
package com.pfe.predictive.alert.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AlertQueryService {

    public long countActiveAlerts() {
        // TODO: implement when Alert entity is created
        return 0;
    }

    public long countCriticalAlerts() {
        // TODO: implement when Alert entity is created
        return 0;
    }
}
```

**Create all 6 query service stubs** (just return 0 or empty lists for now)

### Step 2.5: Test Dashboard

```bash
# Generate JWT token (in your auth controller later, for now use hardcoded)
TOKEN="eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJtYW5hZ2VyIiwicm9sZSI6IlJPTEVfTUFOQUdFUiIsImlhdCI6MTcwNDQ0NDAwMCwiZXhwIjoxNzA0NTMwNDAwfQ...."

# Test dashboard endpoint
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/dashboard | jq

# Should return:
# {
#   "role": "MANAGER",
#   "managerDashboard": { ... }
# }
```

✅ **Phase 2 Complete**: Unified Dashboard endpoint

---

## PHASE 3: Inventory Feature (Days 4-5)

### Step 3.1: Create Entities

Already provided. Copy to:
```
inventory-module/src/main/java/com/pfe/predictive/inventory/entity/InventoryEntities.java
```

Rename classes:
- Part → Part
- InventoryUsage → InventoryUsage  
- ReorderRequest → ReorderRequest
- StockOrder → StockOrder

And create separate files for each entity.

### Step 3.2: Create Repositories

Already provided. Copy to:
```
inventory-module/src/main/java/com/pfe/predictive/inventory/repository/InventoryRepositories.java
```

Break into separate files:
- PartRepository.java
- InventoryUsageRepository.java
- ReorderRequestRepository.java
- StockOrderRepository.java

### Step 3.3: Create Services

Already provided. Copy to:
```
inventory-module/src/main/java/com/pfe/predictive/inventory/service/InventoryServices.java
```

Break into separate files:
- PartService.java (≈150 lines)
- ReorderService.java (≈120 lines)
- InventoryAnalyticsService.java (≈80 lines)

### Step 3.4: Create Mappers

Already provided. Copy to:
```
inventory-module/src/main/java/com/pfe/predictive/inventory/mapper/InventoryMappers.java
```

Break into separate files:
- PartMapper.java
- InventoryUsageMapper.java
- ReorderMapper.java
- StockOrderMapper.java

### Step 3.5: Create Controller

Already provided. Copy to:
```
api-module/src/main/java/com/pfe/predictive/inventory/controller/InventoryController.java
```

### Step 3.6: Create DTOs

Already provided. Copy to:
```
inventory-module/src/main/java/com/pfe/predictive/inventory/dto/InventoryDtos.java
```

### Step 3.7: Create Flyway Migration

```sql
-- File: data-module/resources/db/migration/V3__inventory.sql

CREATE TABLE parts (
    id BIGINT PRIMARY KEY GENERATED BY DEFAULT AS IDENTITY,
    part_number VARCHAR(100) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    category VARCHAR(100),
    cost DECIMAL(10,2),
    current_stock INT NOT NULL DEFAULT 0,
    minimum_stock INT NOT NULL DEFAULT 0,
    reorder_quantity INT,
    unit VARCHAR(50),
    supplier VARCHAR(255),
    status VARCHAR(50) NOT NULL DEFAULT 'AVAILABLE',
    notes VARCHAR(1000),
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_date TIMESTAMP,
    version BIGINT DEFAULT 0,
    CONSTRAINT check_stock_positive CHECK (current_stock >= 0)
);

CREATE TABLE inventory_usage (
    id BIGINT PRIMARY KEY GENERATED BY DEFAULT AS IDENTITY,
    part_id BIGINT NOT NULL,
    quantity_used INT NOT NULL,
    task_id BIGINT,
    reason VARCHAR(50),
    used_by VARCHAR(100),
    notes VARCHAR(1000),
    used_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (part_id) REFERENCES parts(id) ON DELETE RESTRICT
);

CREATE TABLE reorder_requests (
    id BIGINT PRIMARY KEY GENERATED BY DEFAULT AS IDENTITY,
    part_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'REQUESTED',
    reason VARCHAR(100),
    requested_by VARCHAR(100),
    approved_by VARCHAR(100),
    notes VARCHAR(1000),
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    approved_date TIMESTAMP,
    version BIGINT DEFAULT 0,
    FOREIGN KEY (part_id) REFERENCES parts(id) ON DELETE RESTRICT
);

CREATE TABLE stock_orders (
    id BIGINT PRIMARY KEY GENERATED BY DEFAULT AS IDENTITY,
    reorder_request_id BIGINT NOT NULL UNIQUE,
    part_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    cost INT,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    supplier_purchase_order VARCHAR(100),
    expected_delivery_date VARCHAR(200),
    delivered_date VARCHAR(200),
    ordered_by VARCHAR(100),
    proof_of_delivery VARCHAR(1000),
    notes VARCHAR(1000),
    ordered_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_date TIMESTAMP,
    version BIGINT DEFAULT 0,
    FOREIGN KEY (reorder_request_id) REFERENCES reorder_requests(id) ON DELETE RESTRICT,
    FOREIGN KEY (part_id) REFERENCES parts(id) ON DELETE RESTRICT
);

CREATE INDEX idx_parts_status ON parts(status);
CREATE INDEX idx_parts_supplier ON parts(supplier);
CREATE INDEX idx_inventory_usage_part_id ON inventory_usage(part_id);
CREATE INDEX idx_reorder_status ON reorder_requests(status);
CREATE INDEX idx_stock_order_status ON stock_orders(status);
```

### Step 3.8: Test Inventory Endpoints

```bash
# Create part
curl -X POST http://localhost:8080/api/v1/inventory/parts \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Bearing XYZ",
    "partNumber": "BRG-001",
    "cost": 150.00,
    "minimumStock": 10
  }'

# Get all parts
curl http://localhost:8080/api/v1/inventory/parts \
  -H "Authorization: Bearer $TOKEN"

# Request reorder
curl -X POST http://localhost:8080/api/v1/inventory/reorder-requests \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "partId": 1,
    "quantity": 20,
    "reason": "LOW_STOCK"
  }'
```

✅ **Phase 3 Complete**: Inventory feature fully implemented

---

## PHASE 4: Scale to Other Features (Days 6-8)

Repeat Inventory pattern for:
- Maintenance (maintenance-module)
- Task (task-module)
- Alert (alert-module)
- Machine (machine-module)
- Prediction (prediction-module)
- User (user-module)

**For each feature**:
1. Create entities (already provided above)
2. Create repositories (use pattern from InventoryRepositories)
3. Create services (<300 lines each, with Query service for Dashboard)
4. Create DTOs (XRequest, XResponse)
5. Create mappers
6. Create controller (max 7 endpoints)
7. Create Flyway migration

### Template for Each Module

Each module should have this structure:

```
<module>-module/
  src/main/
    java/com/pfe/predictive/<module>/
      entity/
        <Entity>Entity.java      (with enums)
      repository/
        <Entity>Repository.java
      service/
        <Entity>Service.java       (@Transactional, writes)
        <Entity>QueryService.java  (@Transactional(readOnly=true), Dashboard only)
      dto/
        <Entity>Dtos.java         (Request/Response classes)
      mapper/
        <Entity>Mapper.java       (entity ↔ DTO conversion)
    resources/
      db/migration/
        V<N>__<module>.sql       (Flyway migration)
```

---

## PHASE 5: Delete Old Code (Days 9-10)

### Step 5.1: Identify Old Code

```bash
# Find all old role-specific controllers
find . -name "*ManagerController.java" -o -name "*TechnicianController.java" \
       -o -name "*StockManagerController.java" -o -name "*DataScientistController.java"

# Find all old insight services
find . -name "*InsightsService.java" -o -name "*DashboardService.java" \
       -o -name "*ReportService.java"

# Find role-specific DTOs
find . -name "*ManagerDto.java" -o -name "*TechnicianDto.java"
```

### Step 5.2: Safe Deletion Checklist

Before deleting a class:
- [ ] New endpoint implements its functionality
- [ ] Tests exist for new endpoint
- [ ] No other class imports it
- [ ] No database references to old code

```bash
# Find all references
grep -r "ManagerInsightsService" --include="*.java"

# If zero results, safe to delete
rm -rf old-service-module/src/.../ManagerInsightsService.java
```

### Step 5.3: Delete in Order

1. Delete old DTOs (low risk)
2. Delete old Mappers (low risk)
3. Delete old Controllers (test after each deletion)
4. Delete old Services (test after each deletion)
5. Delete old Repositories (if not used)

### Step 5.4: Verify No Broken Imports

```bash
mvn clean compile 2>&1 | grep -i "cannot find symbol"
# Should return zero errors
```

✅ **Phase 5 Complete**: Old code cleaned up

---

## PHASE 6: Validation & Deployment (Days 11-12)

### Step 6.1: Run Full Test Suite

```bash
# Unit tests
mvn test

# Integration tests
mvn verify

# Code quality
mvn sonar:sonar (if SonarQube configured)
```

### Step 6.2: Load Test

```bash
# Using Apache JMeter or similar
# Send 10,000 requests/sec to /api/v1/dashboard for 5 minutes
# Monitor:
# - Response time (should be <100ms for dashboard)
# - Error rate (should be 0%)
# - Memory usage (should stay constant)
```

### Step 6.3: Permission Matrix Verification

Test each role against each endpoint:

```bash
# Create test users for each role
TOKEN_TECHNICIAN=$(curl ... /auth/login with technician creds)
TOKEN_MANAGER=$(curl ... /auth/login with manager creds)
TOKEN_STOCK_MANAGER=$(curl ... /auth/login with stock manager creds)
# ... etc for all 6 roles

# Test TECHNICIAN on MANAGER-only endpoint (should get 403)
curl -H "Authorization: Bearer $TOKEN_TECHNICIAN" \
  http://localhost:8080/api/v1/reorder-requests/1/approve
# Expected: 403 Forbidden

# Test MANAGER on MANAGER endpoint (should succeed)
curl -X POST -H "Authorization: Bearer $TOKEN_MANAGER" \
  http://localhost:8080/api/v1/reorder-requests/1/approve
# Expected: 200 OK or 400 Bad Request (not 403)
```

### Step 6.4: Pre-Deployment Checklist

- [ ] All modules compile: `mvn clean install -DskipTests`
- [ ] All tests pass: `mvn test`
- [ ] No compilation warnings
- [ ] Permission matrix tested (all 6 roles × key endpoints)
- [ ] Dashboard returns correct role-specific data
- [ ] Load test passed (10k req/sec for 5 min)
- [ ] Database migrations applied cleanly
- [ ] No unused services/DTOs/controllers in codebase
- [ ] API documentation updated
- [ ] Team trained on new architecture

### Step 6.5: Deployment

```bash
# Build production JAR
mvn clean package -DskipTests

# Deploy
java -jar target/api-module-1.0.0.jar

# Verify
curl http://localhost:8080/actuator/health
# Should respond with status=UP
```

✅ **Phase 6 Complete**: Validation & Deployment

---

## Timeline Summary

| Phase | Days | Deliverables |
|-------|------|--------------|
| 1: Foundation | 1-2 | PermissionConstants, JWT, SecurityConfig, Config files |
| 2: Dashboard | 3 | Unified /api/v1/dashboard endpoint |
| 3: Inventory | 4-5 | Complete Inventory module (7 endpoints) |
| 4: Scale | 6-8 | Maintenance, Task, Alert, Machine, Prediction, User modules |
| 5: Cleanup | 9-10 | Delete old code, fix imports |
| 6: Validation | 11-12 | Testing, load testing, deployment |

**Total**: 12 days for complete refactoring

---

## Troubleshooting

### Problem: Compilation fails with "cannot find symbol"

```
Solution: Check that all repository interfaces are created and autowired in services
mvn clean compile
```

### Problem: Dashboard returns null for role data

```
Solution: Ensure query service stubs return non-null values
@Service
public class AlertQueryService {
    public long countActiveAlerts() {
        return 0L;  // Must return 0, not null
    }
}
```

### Problem: Permission denied (403) on allowed endpoints

```
Solution: Check POST body contains required fields; 400 is NOT 403
Also verify JWT token contains correct role claim
```

### Problem: Flyway migration fails

```
Solution: Check SQL syntax and column types
Use V<N>__<name>.sql naming convention exactly
```

---

**Ready to start? Begin with Phase 1 and follow sequentially. Good luck!**
