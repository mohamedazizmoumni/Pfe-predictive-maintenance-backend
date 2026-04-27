# Migration Reference & Endpoint Mapping

## Old Endpoints → New Endpoints

### TECHNICIAN Routes

| Old Endpoint | New Endpoint | Status | Notes |
|--------------|--------------|--------|-------|
| GET /api/technician/history | GET /api/v1/dashboard | REPLACED | All data now in single endpoint |
| GET /api/technician/assigned-tasks | GET /api/v1/task?assignedTo=me | CHANGED | Query param added |
| POST /api/technician/use-part | POST /api/v1/inventory/usage | RENAMED | Same functionality |
| POST /api/technician/request-reorder | POST /api/v1/inventory/reorder-requests | RENAMED | Same functionality |
| DELETE /api/technician/task/{id} | DELETE /api/v1/task/{id} | MOVED | Same permission level |

---

### MANAGER Routes

| Old Endpoint | New Endpoint | Status | Notes |
|--------------|--------------|--------|-------|
| GET /api/manager-insights/kpis | GET /api/v1/dashboard | REPLACED | KPIs now in dashboard |
| GET /api/manager-insights/machines | GET /api/v1/machine?availability=true | REPLACED | Analytics endpoint |
| GET /api/manager-insights/alerts | GET /api/v1/alert?status=NEW | REPLACED | Filter by status |
| POST /api/manager/approve-reorder | POST /api/v1/inventory/reorder-requests/{id}/approve | RENAMED | Same functionality |
| GET /api/manager/pending-approvals | GET /api/v1/inventory/reorder-requests/pending | RENAMED | Same permission |
| POST /api/manager/create-maintenance | POST /api/v1/maintenance | RENAMED | Same permission |
| PUT /api/manager/update-task/{id} | PUT /api/v1/task/{id} | RENAMED | Same permission |

---

### STOCK MANAGER Routes

| Old Endpoint | New Endpoint | Status | Notes |
|--------------|--------------|--------|-------|
| GET /api/stock-manager/dashboard | GET /api/v1/dashboard | REPLACED | All inventory stats in dashboard |
| GET /api/stock-manager/low-stock | GET /api/v1/inventory/low-stock | RENAMED | Same functionality |
| GET /api/stock-manager/inventory-stats | GET /api/v1/inventory/stats | RENAMED | Same data |
| POST /api/stock-manager/create-part | POST /api/v1/inventory/parts | RENAMED | Same permission |
| PUT /api/stock-manager/update-part/{id} | PUT /api/v1/inventory/parts/{id} | RENAMED | Same permission |
| DELETE /api/stock-manager/delete-part/{id} | DELETE /api/v1/inventory/parts/{id} | RENAMED | Same permission |
| POST /api/stock-manager/create-order | POST /api/v1/stock-order | RENAMED | New endpoint |
| PUT /api/stock-manager/receive-order/{id} | PUT /api/v1/stock-order/{id}/receive | RENAMED | Same functionality |

---

### DATA SCIENTIST Routes

| Old Endpoint | New Endpoint | Status | Notes |
|--------------|--------------|--------|-------|
| GET /api/data-scientist/models | GET /api/v1/dashboard | REPLACED | Model metrics now in dashboard |
| GET /api/data-scientist/predictions | GET /api/v1/prediction | RENAMED | Same functionality |
| POST /api/data-scientist/train-model | POST /api/v1/ml-model/train | RENAMED | Same permission |
| GET /api/data-scientist/sensor-data/{machineId} | GET /api/v1/sensor-data/{machineId} | RENAMED | Same permission |
| PUT /api/data-scientist/deploy-model/{id} | PUT /api/v1/ml-model/{id}/deploy | RENAMED | Same permission |

---

### ADMIN Routes

| Old Endpoint | New Endpoint | Status | Notes |
|--------------|--------------|--------|-------|
| GET /api/admin/dashboard | GET /api/v1/dashboard | REPLACED | System status in dashboard |
| GET /api/admin/users | GET /api/v1/user | RENAMED | Same functionality |
| POST /api/admin/create-user | POST /api/v1/user | RENAMED | Same permission |
| PUT /api/admin/update-user/{id} | PUT /api/v1/user/{id} | RENAMED | Same permission |
| DELETE /api/admin/delete-user/{id} | DELETE /api/v1/user/{id} | RENAMED | Same permission |
| GET /api/admin/audit-logs | GET /api/v1/audit-logs | RENAMED | Same permission |

---

### SUPER_ADMIN Routes

| Old Endpoint | New Endpoint | Status | Notes |
|--------------|--------------|--------|-------|
| GET /api/super-admin/dashboard | GET /api/v1/dashboard | REPLACED | Full system status in dashboard |
| GET /api/super-admin/system-config | GET /api/v1/system-config | RENAMED | Same permission |
| POST /api/super-admin/backup | POST /api/v1/backup | RENAMED | Same permission |
| PUT /api/super-admin/user/{id}/role | PUT /api/v1/user/{id}/role | RENAMED | Same permission |

---

## Role Permissions Matrix

### Endpoint Access by Role

| Endpoint | TECH | MANAGER | STOCK_MGR | DATA_SCI | ADMIN | SUPER_ADMIN |
|----------|------|---------|-----------|----------|-------|-------------|
| GET /api/v1/dashboard | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| POST /api/v1/maintenance | ❌ | ✅ | ❌ | ❌ | ✅ | ✅ |
| POST /api/v1/inventory/parts | ❌ | ❌ | ✅ | ❌ | ✅ | ✅ |
| POST /api/v1/reorder-requests | ✅ | ✅ | ✅ | ❌ | ✅ | ✅ |
| POST /api/v1/reorder-requests/{id}/approve | ❌ | ✅ | ❌ | ❌ | ✅ | ✅ |
| POST /api/v1/stock-order | ❌ | ❌ | ✅ | ❌ | ✅ | ✅ |
| GET /api/v1/prediction | ❌ | ✅ | ❌ | ✅ | ✅ | ✅ |
| POST /api/v1/ml-model/train | ❌ | ❌ | ❌ | ✅ | ✅ | ✅ |
| POST /api/v1/user | ❌ | ❌ | ❌ | ❌ | ✅ | ✅ |
| PUT /api/v1/user/{id}/role | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ |

---

## Code References

### Old Code to Delete

#### Services (20+ files)
```
ManagerInsightsService.java
ManagerDashboardService.java  
TechnicianSupportService.java
TechnicianDashboardService.java
StockManagerService.java
DataScientistService.java
AdminDashboardService.java
ReportingService.java
InsightsService.java
AnalyticsService.java (old version)
NotificationService.java (if role-specific)
... (12+ more)
```

#### Controllers (8+ files)
```
AdminController.java
AdminOperationsController.java
ManagerController.java
ManagerInsightsController.java
TechnicianController.java
TechnicianSupportController.java
StockManagerController.java
DataScientistController.java
```

#### DTOs (30+ files)
```
ManagerDashboardDto.java
TechnicianHistoryDto.java
DataScientistMetricsDto.java
ManagerPartDto.java
TechnicianPartDto.java
AdminPartDto.java
ManagerMaintenanceDto.java
TechnicianMaintenanceDto.java
... (22+ more)
```

#### Repositories (if unused)
```
Any *InsightsRepository
Any *DashboardRepository
Any *ReportRepository
```

---

## Deletion Order & Safety

### 1. DTOs First (Low Risk)
```bash
# Search for usages
grep -r "ManagerDashboardDto" --include="*.java"

# If only found in old controllers, safe to delete
rm ManagerDashboardDto.java
```

### 2. Mappers Second (Low Risk)
```bash
# Check for usages
grep -r "ManagerDtoMapper" --include="*.java"

# Delete if not used
rm ManagerDtoMapper.java
```

### 3. Services Third (Medium Risk)
```bash
# Search for @Autowired injections
grep -r "ManagerInsightsService" --include="*.java"

# Delete only if:
# 1. All usages moved to new services
# 2. Tests pass
grep -r "ManagerInsightsService" --include="*.java" && echo "UNSAFE" || rm ManagerInsightsService.java
```

### 4. Controllers Last (High Risk)
```bash
# Ensure all endpoints moved to new controllers
grep -r "AdminController" --include="*.java"

# Before deletion:
# 1. Run integration tests
# 2. Verify routing works
# 3. Check no hidden references (configs, etc.)

rm AdminController.java
```

---

## Pre-Deletion Verification Script

```bash
#!/bin/bash
# verify-old-code.sh

CLASSES_TO_DELETE=(
    "ManagerInsightsService"
    "TechnicianSupportService"
    "AdminController"
    "ManagerDashboardDto"
)

for CLASS in "${CLASSES_TO_DELETE[@]}"; do
    echo "Checking references to: $CLASS"
    REFS=$(grep -r "$CLASS" --include="*.java" .)
    
    if [ -z "$REFS" ]; then
        echo "✅ Safe to delete: $CLASS"
    else
        echo "❌ UNSAFE - References found:"
        echo "$REFS"
    fi
done

# Compile check
echo ""
echo "Compiling to check for errors..."
mvn clean compile 2>&1 | grep -i "error"
```

---

## Post-Deletion Verification

### 1. Compilation Test
```bash
mvn clean compile

# Should have 0 compilation errors
echo "Exit code: $?"  # Should be 0
```

### 2. Unit Tests
```bash
mvn test

# Should pass all tests
echo "Exit code: $?"  # Should be 0
```

### 3. Integration Tests
```bash
mvn verify

# Should have 0 integration failures
echo "Exit code: $?"  # Should be 0
```

### 4. JAR Build
```bash
mvn clean package -DskipTests

# Should create WAR/JAR file
ls -la target/*.jar

echo "File size: $(ls -lh target/*.jar | awk '{print $5}')"
```

### 5. Runtime Test
```bash
java -jar target/api-module-1.0.0.jar &

# Wait for startup
sleep 5

# Test health endpoint
curl -s http://localhost:8080/actuator/health | jq .

# Expected output: { "status": "UP", ... }

# Stop
pkill -f "api-module"
```

---

## Permission Verification Checklist

### Before Each Deployment

- [ ] All @PreAuthorize use PermissionConstants only
- [ ] No hardcoded role strings in @PreAuthorize
- [ ] PermissionConstants covers all endpoints
- [ ] Dashboard returns role-specific data
- [ ] Each role tested against key endpoints
- [ ] 403 Forbidden returned for unauthorized access
- [ ] 200 OK returned for authorized access
- [ ] No test accounts with elevated permissions

### Test Matrix Template

```bash
# For each role and endpoint combination:

ROLE="TECHNICIAN"
TOKEN=$(login_as_user $ROLE)
ENDPOINT="/api/v1/inventory/parts"
PERMISSION="REQUIRED: STOCK_MANAGER"

RESPONSE=$(curl -s -w "\n%{http_code}" \
  -H "Authorization: Bearer $TOKEN" \
  $ENDPOINT)

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | head -n-1)

if [ "$HTTP_CODE" = "403" ]; then
    echo "✅ PASS: $ROLE denied access to $ENDPOINT"
else
    echo "❌ FAIL: $ROLE got $HTTP_CODE (expected 403) on $ENDPOINT"
fi
```

---

## Database Migration Verification

### Check Migrations Applied

```sql
-- Connect to database
psql -U postgres -d predictive_maintenance

-- View applied migrations
SELECT * FROM flyway_schema_history;

-- Should show:
-- V1__init.sql
-- V2__update_users_role_constraint.sql
-- V3__inventory.sql
-- ... (etc)
```

### Verify Schema

```sql
-- Check tables exist
\dt

-- Check indexes exist
\di

-- Check constraints
\d+ parts

-- Expected output:
-- Table "public.parts"
-- Column | Type | Nullable | Index | ...
-- id | bigint | not null | PRIMARY KEY | ...
```

---

## Rollback Plan

If deployment fails:

1. **Database Rollback**: Revert last Flyway migration
   ```bash
   # Delete last migration file
   rm V<N>__<name>.sql
   
   # Restart application
   # Flyway will NOT run deleted migrations
   ```

2. **Code Rollback**: Restore previous Git commit
   ```bash
   git revert HEAD
   git push
   ```

3. **Docker Rollback**: Restart previous image
   ```bash
   docker-compose down
   docker-compose up -d  # Uses previous image version
   ```

---

**Use this guide during migration. Verify each step before proceeding to next phase.**
