# Predictive Maintenance Backend - Complete Integration Guide

## Overview

Production-ready Spring Boot 3.x modular monolith with 7 modules delivering complete predictive maintenance system. All services are <300 LOC, role-based access controlled, and database-backed with Flyway migrations.

## Quick Start

### Prerequisites
- Java 17+
- Maven 3.8+
- PostgreSQL 12+ (or MySQL 8+)
- Git

### Build & Run

```bash
# Clone repository
git clone <repo-url>
cd Pfe-predictive-maintenance-backend

# Configure database in application.yml
# Set spring.datasource.url, username, password

# Build all modules
mvn clean package

# Run application
java -jar api-module/target/api-module-1.0.0.jar

# Server starts on http://localhost:8080
# Flyway migrations run automatically on startup
```

---

## Architecture Overview

```
Controller Layer (API)
    ↓
Service Layer (Business Logic)
    ├─→ Write Services (@Transactional)
    └─→ Query Services (@Transactional(readOnly=true))
        ↓
Repository Layer (Data Access)
    ↓
Entity Layer (Domain Models)
    ↓
Database (PostgreSQL/MySQL)
```

### Module Dependencies

```
User Module (Core - No Dependencies)
    ↓
Machine Module (Depends on User)
    ↓
├─→ Maintenance Module (Depends on Machine, User)
├─→ Inventory Module (Depends on User)
├─→ Alert Module (Depends on Machine, User)
├─→ Prediction Module (Depends on Machine)
    ↓
Dashboard Module (Aggregates All)
```

---

## Role-Based Access Control

### 6 Predefined Roles

| Role | Hierarchy | Permissions | Use Case |
|------|-----------|-------------|----------|
| TECHNICIAN | Level 1 | View machines, record data, acknowledge alerts | Field operator |
| STOCK_MANAGER | Level 1 | Manage inventory, create reorders | Warehouse staff |
| DATA_SCIENTIST | Level 1 | Access predictions, view ML models | Analytics team |
| MANAGER | Level 2 | Full read access, approve maintenance | Department lead |
| ADMIN | Level 3 | User management, role assignment, system config | System administrator |
| SUPER_ADMIN | Level 4 | All operations including user deletion, seed data | Infrastructure only |

### Permission Constants (Centralized)

All permissions defined in `security.PermissionConstants`:

```java
// User Management
PERM_USER_READ = "hasAnyRole('MANAGER','ADMIN','SUPER_ADMIN')"
PERM_USER_CREATE = "hasAnyRole('ADMIN','SUPER_ADMIN')"
PERM_USER_UPDATE = "hasAnyRole('ADMIN','SUPER_ADMIN')"
PERM_USER_DELETE = "hasRole('SUPER_ADMIN')"
PERM_USER_ASSIGN_ROLE = "hasRole('SUPER_ADMIN')"

// Machine Management
PERM_MACHINE_READ = "hasAnyRole('TECHNICIAN','MANAGER','DATA_SCIENTIST','ADMIN','SUPER_ADMIN')"
PERM_MACHINE_CREATE = "hasAnyRole('ADMIN','SUPER_ADMIN')"
PERM_MACHINE_UPDATE = "hasAnyRole('TECHNICIAN','MANAGER','ADMIN','SUPER_ADMIN')"

// Maintenance Management
PERM_MAINTENANCE_READ = "hasAnyRole('TECHNICIAN','MANAGER','ADMIN','SUPER_ADMIN')"
PERM_MAINTENANCE_CREATE = "hasAnyRole('MANAGER','ADMIN','SUPER_ADMIN')"
PERM_MAINTENANCE_UPDATE = "hasAnyRole('MANAGER','ADMIN','SUPER_ADMIN')"
PERM_MAINTENANCE_APPROVE = "hasAnyRole('MANAGER','ADMIN','SUPER_ADMIN')"

// Inventory Management
PERM_INVENTORY_READ = "hasAnyRole('STOCK_MANAGER','MANAGER','ADMIN','SUPER_ADMIN')"
PERM_INVENTORY_CREATE = "hasAnyRole('STOCK_MANAGER','ADMIN','SUPER_ADMIN')"
PERM_INVENTORY_UPDATE = "hasAnyRole('STOCK_MANAGER','ADMIN','SUPER_ADMIN')"

// Alert Management
PERM_ALERT_READ = "hasAnyRole('TECHNICIAN','MANAGER','ADMIN','SUPER_ADMIN')"
PERM_ALERT_CREATE = "hasAnyRole('TECHNICIAN','ADMIN','SUPER_ADMIN')"
PERM_ALERT_UPDATE = "hasAnyRole('TECHNICIAN','MANAGER','ADMIN','SUPER_ADMIN')"
PERM_ALERT_ESCALATE = "hasAnyRole('MANAGER','ADMIN','SUPER_ADMIN')"

// Prediction Management
PERM_PREDICTION_READ = "hasAnyRole('DATA_SCIENTIST','MANAGER','ADMIN','SUPER_ADMIN')"
PERM_PREDICTION_CREATE = "hasAnyRole('DATA_SCIENTIST','ADMIN','SUPER_ADMIN')"

// Dashboard
PERM_DASHBOARD_READ = "isAuthenticated()"
```

---

## Complete API Reference

### 1. USER MODULE (`/api/v1/users`)

**Endpoints: 12 | Services: 2 (1 write, 1 read) | Entity: 1**

#### User Management Endpoints

| Method | Endpoint | Permission | Description |
|--------|----------|-----------|-------------|
| GET | `/api/v1/users` | PERM_USER_READ | List all users (paginated) |
| GET | `/api/v1/users/{id}` | PERM_USER_READ | Get user details |
| GET | `/api/v1/users/stats` | PERM_USER_READ | User statistics |
| POST | `/api/v1/users` | PERM_USER_CREATE | Create new user |
| PUT | `/api/v1/users/{id}` | PERM_USER_UPDATE | Update profile |
| POST | `/api/v1/users/{id}/change-password` | PERM_USER_UPDATE | Change password |
| POST | `/api/v1/users/{id}/roles/assign` | PERM_USER_ASSIGN_ROLE | Assign role (SUPER_ADMIN) |
| POST | `/api/v1/users/{id}/roles/remove` | PERM_USER_ASSIGN_ROLE | Remove role (SUPER_ADMIN) |
| POST | `/api/v1/users/{id}/deactivate` | PERM_USER_UPDATE | Deactivate account |
| POST | `/api/v1/users/{id}/reactivate` | PERM_USER_UPDATE | Reactivate account |
| POST | `/api/v1/users/{id}/unlock` | PERM_USER_UPDATE | Unlock locked account |
| DELETE | `/api/v1/users/{id}` | PERM_USER_DELETE | Delete user (SUPER_ADMIN) |

#### Example Requests

```bash
# Create User
curl -X POST http://localhost:8080/api/v1/users \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john.doe",
    "email": "john@example.com",
    "password": "SecurePass123!",
    "firstName": "John",
    "lastName": "Doe",
    "department": "Engineering",
    "roles": ["TECHNICIAN"]
  }'

# List Users
curl -X GET "http://localhost:8080/api/v1/users?page=0&size=20" \
  -H "Authorization: Bearer $TOKEN"

# Assign Role
curl -X POST http://localhost:8080/api/v1/users/1/roles/assign \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"role": "MANAGER", "assign": true}'

# Change Password
curl -X POST http://localhost:8080/api/v1/users/1/change-password \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "currentPassword": "OldPass123!",
    "newPassword": "NewPass456!",
    "confirmPassword": "NewPass456!"
  }'
```

---

### 2. MACHINE MODULE (`/api/v1/machines`)

**Endpoints: 11 | Services: 2 (1 write, 1 read) | Entities: 3 (Machine, Sensor, SensorData)**

#### Machine & Sensor Management

| Method | Endpoint | Permission | Description |
|--------|----------|-----------|-------------|
| GET | `/api/v1/machines` | PERM_MACHINE_READ | List machines (with filters) |
| GET | `/api/v1/machines/{id}` | PERM_MACHINE_READ | Get machine details |
| GET | `/api/v1/machines/stats/overview` | PERM_MACHINE_READ | Machine statistics |
| POST | `/api/v1/machines` | PERM_MACHINE_CREATE | Create machine |
| PUT | `/api/v1/machines/{id}` | PERM_MACHINE_UPDATE | Update machine |
| POST | `/api/v1/machines/{id}/status` | PERM_MACHINE_UPDATE | Change status |
| POST | `/api/v1/machines/{id}/maintenance` | PERM_MACHINE_UPDATE | Mark for maintenance |
| GET | `/api/v1/machines/{machineId}/sensors` | PERM_MACHINE_READ | List machine sensors |
| POST | `/api/v1/machines/{machineId}/sensors` | PERM_MACHINE_UPDATE | Add sensor |
| PUT | `/api/v1/machines/{machineId}/sensors/{sensorId}` | PERM_MACHINE_UPDATE | Update sensor |
| DELETE | `/api/v1/machines/{machineId}/sensors/{sensorId}` | PERM_MACHINE_UPDATE | Remove sensor |

#### Sensor Data Endpoints

| Method | Endpoint | Permission | Description |
|--------|----------|-----------|-------------|
| POST | `/api/v1/machines/{machineId}/sensors/{sensorId}/readings` | PERM_MACHINE_CREATE | Record reading |
| GET | `/api/v1/machines/{machineId}/sensors/{sensorId}/readings` | PERM_MACHINE_READ | Get recent readings |
| GET | `/api/v1/machines/{machineId}/sensors/{sensorId}/anomalies` | PERM_MACHINE_READ | Get anomalies |
| GET | `/api/v1/machines/{machineId}/sensors/{sensorId}/health` | PERM_MACHINE_READ | Sensor health |

#### Example Requests

```bash
# Create Machine
curl -X POST http://localhost:8080/api/v1/machines \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "serialNumber": "MACH-001-2024",
    "model": "CNC-5000",
    "location": "Floor A - Station 5",
    "manufacturer": "Siemens",
    "installationYear": 2023
  }'

# List Machines
curl -X GET "http://localhost:8080/api/v1/machines?status=OPERATIONAL&page=0&size=10" \
  -H "Authorization: Bearer $TOKEN"

# Add Sensor
curl -X POST http://localhost:8080/api/v1/machines/1/sensors \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "sensorType": "TEMPERATURE",
    "unit": "Celsius",
    "minThreshold": 15.0,
    "maxThreshold": 45.0
  }'

# Record Sensor Reading
curl -X POST http://localhost:8080/api/v1/machines/1/sensors/1/readings \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"value": 38.5}'

# Get Sensor Readings
curl -X GET "http://localhost:8080/api/v1/machines/1/sensors/1/readings?page=0&size=50" \
  -H "Authorization: Bearer $TOKEN"
```

---

### 3. MAINTENANCE MODULE (`/api/v1/maintenance`)

**Endpoints: 8 | Services: 2 | Entities: 1**

| Method | Endpoint | Permission | Description |
|--------|----------|-----------|-------------|
| GET | `/api/v1/maintenance` | PERM_MAINTENANCE_READ | List maintenance tasks |
| GET | `/api/v1/maintenance/{id}` | PERM_MAINTENANCE_READ | Get task details |
| POST | `/api/v1/maintenance` | PERM_MAINTENANCE_CREATE | Create task |
| PUT | `/api/v1/maintenance/{id}` | PERM_MAINTENANCE_UPDATE | Update task |
| POST | `/api/v1/maintenance/{id}/start` | PERM_MAINTENANCE_UPDATE | Start maintenance |
| POST | `/api/v1/maintenance/{id}/complete` | PERM_MAINTENANCE_UPDATE | Complete task |
| POST | `/api/v1/maintenance/{id}/approve` | PERM_MAINTENANCE_APPROVE | Approve completion |
| DELETE | `/api/v1/maintenance/{id}` | PERM_MAINTENANCE_UPDATE | Cancel task |

---

### 4. INVENTORY MODULE (`/api/v1/inventory`)

**Endpoints: 10 | Services: 3 | Entities: 4**

| Method | Endpoint | Permission | Description |
|--------|----------|-----------|-------------|
| GET | `/api/v1/inventory/parts` | PERM_INVENTORY_READ | List parts |
| GET | `/api/v1/inventory/parts/{id}` | PERM_INVENTORY_READ | Get part details |
| POST | `/api/v1/inventory/parts` | PERM_INVENTORY_CREATE | Add part |
| PUT | `/api/v1/inventory/parts/{id}` | PERM_INVENTORY_UPDATE | Update part |
| DELETE | `/api/v1/inventory/parts/{id}` | PERM_INVENTORY_UPDATE | Remove part |
| GET | `/api/v1/inventory/reorders` | PERM_INVENTORY_READ | List reorder requests |
| POST | `/api/v1/inventory/reorders` | PERM_INVENTORY_CREATE | Create reorder |
| POST | `/api/v1/inventory/reorders/{id}/approve` | PERM_INVENTORY_UPDATE | Approve reorder |
| GET | `/api/v1/inventory/analytics` | PERM_INVENTORY_READ | Inventory analytics |
| POST | `/api/v1/inventory/consumed` | PERM_INVENTORY_UPDATE | Record consumption |

---

### 5. ALERT MODULE (`/api/v1/alerts`)

**Endpoints: 7 | Services: 2 | Entities: 1**

| Method | Endpoint | Permission | Description |
|--------|----------|-----------|-------------|
| GET | `/api/v1/alerts` | PERM_ALERT_READ | List alerts |
| GET | `/api/v1/alerts/{id}` | PERM_ALERT_READ | Get alert details |
| GET | `/api/v1/alerts/stats` | PERM_ALERT_READ | Alert statistics |
| POST | `/api/v1/alerts` | PERM_ALERT_CREATE | Create alert |
| PUT | `/api/v1/alerts/{id}/acknowledge` | PERM_ALERT_UPDATE | Acknowledge alert |
| POST | `/api/v1/alerts/{id}/escalate` | PERM_ALERT_ESCALATE | Escalate alert |
| POST | `/api/v1/alerts/{id}/close` | PERM_ALERT_UPDATE | Close alert |

---

### 6. PREDICTION MODULE (`/api/v1/predictions`)

**Endpoints: 6 | Services: 2 | Entities: 2**

| Method | Endpoint | Permission | Description |
|--------|----------|-----------|-------------|
| GET | `/api/v1/predictions` | PERM_PREDICTION_READ | List predictions |
| GET | `/api/v1/predictions/{id}` | PERM_PREDICTION_READ | Get prediction |
| POST | `/api/v1/predictions` | PERM_PREDICTION_CREATE | Create prediction |
| GET | `/api/v1/predictions/models` | PERM_PREDICTION_READ | List ML models |
| PUT | `/api/v1/predictions/models/{id}` | PERM_PREDICTION_CREATE | Update model |
| GET | `/api/v1/predictions/trends` | PERM_PREDICTION_READ | Get trends |

---

### 7. DASHBOARD MODULE (`/api/v1/dashboard`)

**Endpoints: 1 | Services: 1 | Role-Specific Responses**

| Method | Endpoint | Permission | Description |
|--------|----------|-----------|-------------|
| GET | `/api/v1/dashboard` | PERM_DASHBOARD_READ | Role-specific dashboard |

**Response varies by role:**

- **TECHNICIAN**: Assigned tasks, assigned alerts, recent sensor readings
- **MANAGER**: Team overview, alert summary by status, maintenance KPIs
- **STOCK_MANAGER**: Low-stock items, pending reorders, stock movement
- **DATA_SCIENTIST**: Model accuracy metrics, prediction trends, anomaly distribution
- **ADMIN**: System health, user statistics, all-time alerts, error rates
- **SUPER_ADMIN**: Full system view including audit logs and infrastructure metrics

---

### 8. CHATBOT MODULE (`/api/v1/chatbot`)

**Endpoints: 1 | Services: 2 | Security: JWT + Role-Aware Authorization**

| Method | Endpoint | Permission | Description |
|--------|----------|-----------|-------------|
| POST | `/api/v1/chatbot/ask` | Authenticated users | Role-based professional assistant response |

Rules enforced by backend:

- Only answers within the current user role scope
- Sensitive topics are blocked (passwords, secrets, tokens, credentials, API keys)
- Forbidden requests return exactly `NOT AUTHORIZED`
- Responses are concise and professional

Request example:

```bash
curl -X POST http://localhost:8080/api/v1/chatbot/ask \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "question": "Give me a summary of current maintenance alerts"
  }'
```

Response example:

```json
{
  "answer": "You currently have 3 open maintenance alerts. Two are high priority and one is medium.",
  "authorized": true,
  "userRoles": ["TECHNICIAN"]
}
```

Unauthorized example:

```json
{
  "answer": "NOT AUTHORIZED",
  "authorized": false,
  "userRoles": ["VIEWER"]
}
```

Frontend integration example (JavaScript/TypeScript):

```ts
export async function askChatbot(question: string, token: string) {
  const response = await fetch("http://localhost:8080/api/v1/chatbot/ask", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`
    },
    body: JSON.stringify({ question })
  });

  if (!response.ok) {
    throw new Error(`Chatbot request failed: ${response.status}`);
  }

  return response.json();
}
```

---

## Database Schema

### Migration Files (Flyway)

| Version | File | Purpose |
|---------|------|---------|
| V1 | `V1__init.sql` | Users table (RBAC foundation) |
| V2 | `V2__update_users_role_constraint.sql` | Role constraints |
| V3 | `V3__create_alerts_table.sql` | Alert monitoring |
| V4 | `V4__create_machines_table.sql` | Machines, sensors, sensor data |
| V5 | `V5__create_maintenance_table.sql` | Maintenance tasks |
| V6 | `V6__create_inventory_tables.sql` | Parts, reorders, stock orders |
| V7 | `V7__create_predictions_table.sql` | ML predictions, models |

### Key Design Patterns

1. **Audit Fields on All Entities**
   ```sql
   created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
   last_modified_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
   created_by VARCHAR(100),
   last_modified_by VARCHAR(100),
   version INTEGER (optimistic locking)
   ```

2. **Status Enums as Check Constraints**
   ```sql
   status VARCHAR(50) CHECK (status IN ('VALUE1', 'VALUE2', ...))
   ```

3. **Cascading Deletes**
   ```sql
   CONSTRAINT fk_field FOREIGN KEY (parent_id) REFERENCES parent(id) ON DELETE CASCADE
   ```

4. **Composite Indexes for Filtering**
   ```sql
   CREATE INDEX idx_sensor_status_timestamp ON sensors(status, created_date DESC)
   ```

---

## Configuration

### application.yml

```yaml
spring:
  application:
    name: predictive-maintenance-backend
  
  datasource:
    url: jdbc:postgresql://localhost:5432/predictive_maintenance
    username: postgres
    password: postgres
    driver-class-name: org.postgresql.Driver
  
  jpa:
    hibernate:
      ddl-auto: validate  # Never auto-create; let Flyway manage
    show-sql: false
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.PostgreSQLDialect
  
  flyway:
    locations: classpath:db/migration
    baseline-on-migrate: true
    out-of-order: false
  
  security:
    jwt:
      secret: ${JWT_SECRET:your-super-secret-jwt-key-change-in-production}
      expiration: 86400000 # 24 hours
      refresh-expiration: 604800000 # 7 days
  
logging:
  level:
    root: INFO
    com.pfe.predictive: DEBUG
  
server:
  port: 8080
  servlet:
    context-path: /
```

### Environment Variables

```bash
# Database
export DB_URL=jdbc:postgresql://localhost:5432/predictive_maintenance
export DB_USER=postgres
export DB_PASSWORD=postgres

# Security (Change in Production!)
export JWT_SECRET=your-complex-secret-key-here-min-256-chars
export OPENAI_API_KEY=your-openai-api-key

# Logging
export LOG_LEVEL=DEBUG
```

---

## Integration Checklist

- [ ] Clone repository
- [ ] Configure database credentials in `application.yml`
- [ ] Set `JWT_SECRET` environment variable
- [ ] Run `mvn clean package` to build all modules
- [ ] Start application: `java -jar api-module/target/api-module-1.0.0.jar`
- [ ] Verify Flyway migrations run on startup
- [ ] Create SUPER_ADMIN user for initial setup
- [ ] Test endpoints with provided curl examples
- [ ] Add module to Spring component scan if not auto-detected
- [ ] Configure SSL/TLS for production
- [ ] Set up audit logging
- [ ] Set `OPENAI_API_KEY` for chatbot integration
- [ ] Deploy to target environment

---

## Module Dependencies & Build Order

### Build all modules:
```bash
mvn clean package
```

### Or build individually:
```bash
# Core - Independent modules (no dependencies)
mvn clean package -pl user-module
mvn clean package -pl core-module  # Contains PermissionConstants

# Data layer
mvn clean package -pl data-module  # Contains Flyway migrations

# Domain modules (depend on core)
mvn clean package -pl machine-module
mvn clean package -pl inventory-module
mvn clean package -pl alert-module

# Higher-level modules
mvn clean package -pl maintenance-module  # Depends: machine, user
mvn clean package -pl ml-module           # Depends: machine, prediction
mvn clean package -pl security-module     # JWT, Spring Security

# API layer
mvn clean package -pl api-module          # Aggregates all controllers
```

### Pom.xml Dependency Tree

```xml
<dependency>
  <groupId>com.pfe.predictive</groupId>
  <artifactId>core-module</artifactId>
  <version>1.0.0</version> <!-- PermissionConstants, enums -->
</dependency>

<dependency>
  <groupId>com.pfe.predictive</groupId>
  <artifactId>machine-module</artifactId>
  <version>1.0.0</version>
</dependency>

<dependency>
  <groupId>com.pfe.predictive</groupId>
  <artifactId>user-module</artifactId>
  <version>1.0.0</version>
</dependency>

<dependency>
  <groupId>com.pfe.predictive</groupId>
  <artifactId>alert-module</artifactId>
  <version>1.0.0</version>
</dependency>

<!-- ... and so on -->
```

---

## Testing & Examples

### Test with Default Admin Account

```bash
# Create super admin on first run
curl -X POST http://localhost:8080/api/v1/auth/seed \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "email": "admin@example.com",
    "password": "AdminPass123!"
  }'

# Login to get JWT token
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "AdminPass123!"
  }'

# Returns:
# {
#   "token": "eyJhbGc...",
#   "refreshToken": "...",
#   "expiresIn": 86400000
# }

# Use token for all authenticated requests
export TOKEN="eyJhbGc..."
curl -X GET http://localhost:8080/api/v1/users \
  -H "Authorization: Bearer $TOKEN"
```

### Postman Collection Example

```json
{
  "info": {
    "name": "Predictive Maintenance API",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "Auth",
      "item": [
        {
          "name": "Login",
          "event": [
            {
              "listen": "test",
              "script": {
                "exec": [
                  "var jsonData = pm.response.json();",
                  "pm.environment.set('token', jsonData.token);"
                ]
              }
            }
          ],
          "request": {
            "method": "POST",
            "header": [
              {
                "key": "Content-Type",
                "value": "application/json"
              }
            ],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"username\": \"admin\",\n  \"password\": \"AdminPass123!\"\n}"
            },
            "url": {
              "raw": "{{base_url}}/api/v1/auth/login",
              "host": ["{{base_url}}"],
              "path": ["api", "v1", "auth", "login"]
            }
          }
        }
      ]
    },
    {
      "name": "Machines",
      "item": [
        {
          "name": "List Machines",
          "request": {
            "method": "GET",
            "header": [
              {
                "key": "Authorization",
                "value": "Bearer {{token}}"
              }
            ],
            "url": {
              "raw": "{{base_url}}/api/v1/machines?page=0&size=10",
              "host": ["{{base_url}}"],
              "path": ["api", "v1", "machines"],
              "query": [
                {"key": "page", "value": "0"},
                {"key": "size", "value": "10"}
              ]
            }
          }
        }
      ]
    }
  ]
}
```

---

## Troubleshooting

### Common Issues

| Issue | Cause | Solution |
|-------|-------|----------|
| Flyway migration fails | Database permissions | Ensure DB user has CREATE TABLE privileges |
| 403 Forbidden on endpoints | Missing or invalid token | Generate new token, verify JWT_SECRET |
| Service not found | Module not in classpath | Verify pom.xml dependency, check jar contents |
| Connection timeout | Database not running | Start PostgreSQL, verify connection string |
| Permission denied on port 8080 | Port in use or privileged | Use different port or run as admin |

### Logs to Check

```bash
# Application logs
tail -f logs/application.log

# Database logs (PostgreSQL)
tail -f /var/log/postgresql/postgresql.log

# Check migrations
SELECT * FROM flyway_schema_history;

# Verify tables created
\dt  -- in psql
```

---

## Deployment Guide

### Production Checklist

- [ ] Set strong JWT_SECRET (min 256 chars random)
- [ ] Disable SQL logging: `spring.jpa.show-sql=false`
- [ ] Set JPA DDL mode to `validate`: `spring.jpa.hibernate.ddl-auto=validate`
- [ ] Enable HTTPS with valid SSL certificate
- [ ] Configure CORS properly for frontend domain
- [ ] Set database pool size appropriately
- [ ] Enable audit logging
- [ ] Set up monitoring/alerting
- [ ] Create database backups
- [ ] Test failover/disaster recovery

### Docker Example

```dockerfile
FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY api-module/target/api-module-1.0.0.jar app.jar

ENV SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/predictive_maintenance
ENV SPRING_DATASOURCE_USERNAME=postgres
ENV SPRING_DATASOURCE_PASSWORD=postgres
ENV JWT_SECRET=production-secret-key

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

## Quick Links

- **Security Config**: `security-module/SecurityConfig.java`
- **Permission Constants**: `core-module/PermissionConstants.java`
- **JWT Utilities**: `security-module/JwtUtils.java`
- **Migrations**: `data-module/src/main/resources/db/migration/`
- **Application Config**: `api-module/src/main/resources/application.yml`

---

## Support & Documentation

For detailed module documentation, see:
- `user-module/README.md` - User management & RBAC
- `machine-module/README.md` - Machine monitoring
- `maintenance-module/README.md` - Maintenance scheduling
- `inventory-module/README.md` - Parts & stock management
- `alert-module/README.md` - Alert system
- `ml-module/README.md` - Predictions & ML models

---

**Backend Version**: 1.0.0  
**Last Updated**: 2024  
**Status**: Production-Ready
