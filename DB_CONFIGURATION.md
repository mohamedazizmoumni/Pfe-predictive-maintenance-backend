# PostgreSQL Configuration Summary

## Quick Reference

```yaml
DATABASE: pfe
USER: postgres
PASSWORD: admin
HOST: localhost
PORT: 5432
DRIVER: org.postgresql.Driver
JDBC_URL: jdbc:postgresql://localhost:5432/pfe
```

---

## Application Configuration Details

### Main Configuration File
**Location**: `api-module/src/main/resources/application.yml`

### Database Section
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/pfe
    username: postgres
    password: admin
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: 10           # Max connections in pool
      minimum-idle: 5                 # Min idle connections
      connection-timeout: 20000       # 20 seconds
      idle-timeout: 300000            # 5 minutes
      test-on-borrow: true            # Test before using
      validation-query: "SELECT 1"
```

### JPA/Hibernate Section
```yaml
  jpa:
    database: POSTGRESQL
    database-platform: org.hibernate.dialect.PostgreSQLDialect
    hibernate:
      ddl-auto: validate              # Don't auto-create tables (use Flyway)
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQL10Dialect
        jdbc:
          fetch-size: 50
          batch-size: 10
```

### Flyway Migration Section
```yaml
  flyway:
    enabled: true
    locations: classpath:db/migration  # Where migration files are located
    baseline-on-migrate: false         # Don't create baseline if DB exists
```

---

## Database Setup Commands

### Create Database (PostgreSQL)

**Using psql**:
```bash
psql -U postgres

CREATE DATABASE pfe
  WITH 
  OWNER = postgres
  ENCODING = 'UTF8'
  LC_COLLATE = 'C'
  LC_CTYPE = 'C'
  TEMPLATE = template0;

\c pfe
\dt  # List tables (should be empty initially)
```

**Using SQL file** (execute in pgAdmin):
```sql
CREATE DATABASE pfe
  WITH 
  OWNER = postgres
  ENCODING = 'UTF8'
  LC_COLLATE = 'C'
  LC_CTYPE = 'C'
  TEMPLATE = template0;
```

---

## Verify Configuration

```bash
# Test connection from command line
psql -U postgres -h localhost -d pfe -c "SELECT version();"

# Expected output:
# PostgreSQL 12.x.x on x86_64-pc-linux-gnu...
```

---

## What Happens on Application Startup

1. ✅ Spring Boot reads `application.yml`
2. ✅ HikariCP creates connection pool to PostgreSQL
3. ✅ Flyway discovers migration files in `classpath:db/migration`
4. ✅ Flyway validates existing migrations
5. ✅ Flyway executes any new migrations in sequence:
   - `V1__init.sql` - Initial schema (users, machines, etc.)
   - `V2__update_users_role_constraint.sql` - Updates to users table
   - `V5__create_maintenance_table.sql` - Maintenance tasks table
   - `V6__create_ml_models_table.sql` - ML models table
   - `V7__create_predictions_table.sql` - Predictions table
6. ✅ JPA validates entities against database schema (mode: validate)
7. ✅ Application starts on port 8080

---

## Connection Pool Configuration

The application uses **HikariCP** (industry standard, fastest):

| Setting | Value | Purpose |
|---------|-------|---------|
| maximum-pool-size | 10 | Max concurrent connections |
| minimum-idle | 5 | Keep 5 connections open |
| connection-timeout | 20s | Wait 20s for available connection |
| idle-timeout | 5min | Close idle connections after 5 min |
| test-on-borrow | true | Validate connection before using |

---

## Logging Configuration

The database operations are logged at:

```
logging:
  level:
    com.pfe.predictive.data: DEBUG     # SQL generation
    org.flywaydb: INFO                  # Migration progress
    org.springframework.data: DEBUG      # JPA operations
    org.hibernate.SQL: DEBUG            # SQL statements
```

Check logs in: `logs/application.log`

---

## Configuration Checklist

- [ ] PostgreSQL 12+ installed and running
- [ ] Database `pfe` created with owner `postgres`
- [ ] `postgres` user password is `admin` (or updated in config)
- [ ] `api-module/src/main/resources/application.yml` is in place
- [ ] Migration files exist in `data-module/src/main/resources/db/migration/`
- [ ] Project builds successfully: `mvn clean install`
- [ ] Application starts: `mvn spring-boot:run -pl api-module`
- [ ] Logs show: `Successfully validated X migrations`
- [ ] Can connect to database: `psql -U postgres -d pfe`

---

## If You Need to Change Credentials

1. Update password in PostgreSQL:
   ```sql
   ALTER USER postgres WITH PASSWORD 'new_password';
   ```

2. Update in `api-module/src/main/resources/application.yml`:
   ```yaml
   spring:
     datasource:
       password: new_password
   ```

3. Rebuild: `mvn clean install`
4. Run: `java -jar api-module/target/api-module-1.0.0.jar`

---

## Database Schema (Auto-Created)

Tables created by Flyway migrations:

```sql
-- Core tables
users (V1)
machines (V1)
sensors (V1)
sensor_data (V1)

-- Feature tables
alerts (V1)
inventory (V1)
reorders (V1)

-- Maintenance & Prediction (Phase 3 additions)
maintenance (V5)
ml_models (V6)
predictions (V7)
```

---

## Troubleshooting

| Error | Cause | Solution |
|-------|-------|----------|
| "Connection refused" | PostgreSQL not running | Start PostgreSQL service |
| "Password auth failed" | Wrong password | Check password in config |
| "Database pfe does not exist" | DB not created | Create database (see Setup Commands) |
| "Flyway migration failed" | Migration file error | Check migration syntax in logs |
| "Relation X does not exist" | Table not created yet | Ensure Flyway ran successfully |

---

## Performance Monitoring

### Check Connection Pool Status

The application exposes metrics at:
```
http://localhost:8080/actuator/metrics/hikaricp.connections
```

### Monitor Database Connections

From PostgreSQL:
```sql
SELECT count(*) as connection_count FROM pg_stat_activity;
```

---

## Next Steps

1. **Create the database**: Run commands in "Database Setup Commands" section
2. **Start PostgreSQL**: Ensure service is running
3. **Build project**: `mvn clean install`
4. **Run application**: `java -jar api-module/target/api-module-1.0.0.jar`
5. **Verify**: Check logs for migration success
6. **Test API**: Use curl or Postman to test endpoints

---

## Useful PostgeSQL Commands

```bash
# Connect to database
psql -U postgres -h localhost -d pfe

# List databases
\l

# List tables in pfe database
\dt

# Show table structure
\d users

# Show column info
\d+ users

# Exit psql
\q

# Backup database
pg_dump -U postgres -h localhost pfe > backup.sql

# Restore database
psql -U postgres -h localhost pfe < backup.sql
```

