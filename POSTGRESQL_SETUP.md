# PostgreSQL Database Setup Guide

## Database Configuration for Predictive Maintenance Backend

### Prerequisites
- PostgreSQL 12 or higher installed
- pgAdmin or psql client available
- Default postgres superuser password set during installation

---

## Step 1: Create Database

### Option A: Using pgAdmin (GUI)

1. Open pgAdmin 4 (usually on `http://localhost:5050`)
2. Connect to local PostgreSQL server
3. Right-click on "Databases" → Create → Database
4. **Database name**: `pfe`
5. **Owner**: `postgres`
6. Click Save

### Option B: Using psql (Command Line)

```bash
# On Windows (Command Prompt or PowerShell)
psql -U postgres

# Enter password: admin (default or your set password)

# Then run:
CREATE DATABASE pfe
  WITH 
  OWNER = postgres
  ENCODING = 'UTF8'
  LC_COLLATE = 'en_US.UTF-8'
  LC_CTYPE = 'en_US.UTF-8'
  TEMPLATE = template0;

# Verify creation
\l

# Exit psql
\q
```

---

## Step 2: Verify User & Password

### Check postgres User

```bash
psql -U postgres -h localhost
# Password: admin
```

### Change Password (if needed)

```sql
ALTER USER postgres WITH PASSWORD 'admin';
```

---

## Step 3: Application Configuration

The application is already configured with:

**Database Connection Details** (in `api-module/src/main/resources/application.yml`):
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/pfe
    username: postgres
    password: admin
    driver-class-name: org.postgresql.Driver
```

**Flyway Migration** (Automatic on Startup):
- Locations: `classpath:db/migration`
- Migration files: `V1__init.sql`, `V2__update_users_role_constraint.sql`, etc.
- These run automatically when the application starts

---

## Step 4: Verify Database Access

Test the connection using DBeaver or pgAdmin:

```
Host: localhost
Port: 5432
Database: pfe
User: postgres
Password: admin
```

---

## Step 5: Build & Run Application

```bash
# Navigate to project root
cd Pfe-predictive-maintenance-backend

# Build all modules
mvn clean install

# Run the application
mvn spring-boot:run -pl api-module

# OR run the JAR directly
java -jar api-module/target/api-module-1.0.0.jar
```

### Expected Output

```
...
o.s.b.a.w.s.WelcomePageHandlerMapping    : Adding welcome page: class path resource [static/index.html]
o.f.c.internal.database.base.BaseConnection : Database: PostgreSQL 12.x.x on x86_64-pc-linux-gnu
o.f.core.internal.command.DbValidate      : Successfully validated 7 migrations (with checksum)
o.s.b.w.embedded.tomcat.TomcatWebServer    : Tomcat started on port(s): 8080 (http)
```

✅ **Application is running!** Access it at: `http://localhost:8080`

---

## Step 6: Test Database Connection

### Test via POST Request

```bash
# Create a user (test database connectivity)
curl -X POST http://localhost:8080/api/v1/users \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "Test@123",
    "role": "TECHNICIAN"
  }'
```

---

## Troubleshooting PostgreSQL Issues

### Issue 1: "Connection Refused" (localhost:5432)

**Solution**: Ensure PostgreSQL server is running
```bash
# Windows - Start PostgreSQL Service
net start postgresql-x64-13

# Linux
sudo systemctl start postgresql

# macOS
brew services start postgresql
```

### Issue 2: "FATAL: password authentication failed"

**Solution**: Reset postgres password in pgAdmin or via SQL:
```sql
ALTER USER postgres WITH PASSWORD 'admin';
```

### Issue 3: "Database 'pfe' does not exist"

**Solution**: Create the database (see Step 1)

### Issue 4: "Flyway Migration Failed"

Check logs and verify:
1. `/data-module/src/main/resources/db/migration/` exists
2. Migration files (V1, V2, etc.) are present
3. No duplicate version numbers

---

## Database Details

### Configured Databases Table

| Database | User | Password | Port | Purpose |
|----------|------|----------|------|---------|
| pfe | postgres | admin | 5432 | Main application database |

### Tables (Auto-Created by Flyway)

```
users
machines
sensors
sensor_data
maintenance
ml_models
predictions
alerts
inventory
reorders
```

---

## Connection String Reference

**Java JDBC**:
```java
String url = "jdbc:postgresql://localhost:5432/pfe";
String user = "postgres";
String password = "admin";
```

**Spring Boot YAML**:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/pfe
    username: postgres
    password: admin
```

**psql Command Line**:
```bash
psql -U postgres -h localhost -d pfe
```

---

## Next Steps

1. ✅ Database created (`pfe`)
2. ✅ Application configured (see `api-module/src/main/resources/application.yml`)
3. **→ Build and run the application**
4. **→ Access API at** `http://localhost:8080`
5. **→ Authenticate with default admin credentials**

---

## Performance Tuning (Optional)

For production, add these to PostgreSQL `postgresql.conf`:

```ini
# Memory
shared_buffers = 256MB
effective_cache_size = 1GB
maintenance_work_mem = 64MB
work_mem = 16MB

# Connections
max_connections = 100

# WAL (Write-Ahead Logging)
wal_buffers = 16MB
checkpoint_completion_target = 0.9
wal_compression = on
```

---

## Support

For issues with:
- **Database**: Check PostgreSQL logs in `PostgreSQL/data/`
- **Application**: Check logs in `logs/application.log`
- **Migrations**: Verify migration file syntax matches Flyway format (V#__description.sql)

