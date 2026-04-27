# Clean Architecture Refactoring Guide

## Overview

This guide provides a **complete, production-ready refactoring** of your Spring Boot modular monolith from a chaotic role-based structure to a clean, maintainable **feature-based architecture**.

---

## 1. Architecture Principles

### Current Problem
- **Role-based Controllers**: AdminController, ManagerController, TechnicianController, etc. (duplicated logic)
- **Service Chaos**: ManagerInsightsService calling 4+ other services; 20+ service classes  
- **DTO Explosion**: 50+ DTOs, one per role + endpoint combination
- **Scattered Permissions**: @PreAuthorize scattered across 50+ methods
- **Mixed Responsibilities**: Inventory, Stock, Reorder flows overlapping

### Target Design
- ✅ **Feature-based Structure**: Each module = one business capability (Inventory, Maintenance, Task, Alert, etc.)
- ✅ **Centralized Permissions**: Single `PermissionConstants.java` for all @PreAuthorize
- ✅ **Unified Dashboard**: One endpoint `/api/v1/dashboard` returns role-specific data
- ✅ **Single Responsibility Services**: Each service <300 lines, one clear purpose
- ✅ **Clean DTOs**: XRequest, XResponse pattern (not role-specific)

---

## 2. Module Structure

```
/api-module
    /dashboard
        DashboardController.java      (single entry point for all roles)
        DashboardService.java         (role-specific aggregation)
        /dto
            DashboardResponse.java    (all role response classes)

/inventory-module
    /entity
        InventoryEntities.java        (Part, InventoryUsage, ReorderRequest, StockOrder)
    /repository
        InventoryRepositories.java
    /service
        PartService.java              (<250 lines: CRUD for parts)
        ReorderService.java           (<250 lines: reorder workflow)
        InventoryAnalyticsService.java (<250 lines: read-only queries for dashboard)
    /mapper
        InventoryMappers.java
    /controller
        InventoryController.java      (7 endpoints, all use @PreAuthorize)
    /dto
        InventoryDtos.java            (Request/Response classes)

/maintenance-module
    /entity
        MaintenanceEntity.java
    /service
        MaintenanceService.java
        MaintenanceQueryService.java
    /repository
        MaintenanceRepository.java
    /controller
        MaintenanceController.java
    /dto
        MaintenanceDtos.java

/task-module, /alert-module, /machine-module, /prediction-module, /user-module
    (same structure as above)

/common-module
    /security
        PermissionConstants.java      (40+ permission constants)
    /config
        SecurityConfig.java           (JWT + Spring Security setup)
    /utils
        AppMapper.java                (utility mapping methods)
```

---

## 3. Key Components

### 3.1 PermissionConstants.java

**Purpose**: Single source of truth for all permission checks

```java
public class PermissionConstants {
    // ROLE DEFINITIONS
    public static final String ROLE_TECHNICIAN = "ROLE_TECHNICIAN";
    public static final String ROLE_MANAGER = "ROLE_MANAGER";
    // ... 4 more roles
    
    // PERMISSION STRINGS
    public static final String PERM_MAINTENANCE_READ = 
        "hasAnyRole('TECHNICIAN','MANAGER','ADMIN','SUPER_ADMIN')";
    public static final String PERM_REORDER_APPROVE = 
        "hasAnyRole('MANAGER','ADMIN','SUPER_ADMIN')";
    // ... 40+ more permissions
    
    // UTILITY METHODS
    public static boolean isSuperAdminOrAdmin(String role) { ... }
    public static boolean canApproveReorders(String role) { ... }
}
```

**Usage in Controller**:
```java
@PostMapping("/reorder-requests/{id}/approve")
@PreAuthorize(PermissionConstants.PERM_REORDER_APPROVE)
public ResponseEntity<ReorderRequestResponse> approveReorder(...) { ... }
```

**Benefit**: Change permission once = affects all endpoints automatically

---

### 3.2 Unified Dashboard Endpoint

**Purpose**: Replace 8 role-specific insights endpoints with ONE endpoint

**Old Endpoints** (DELETE THESE):
```
GET /api/manager-insights/kpis
GET /api/technician/history
GET /api/data-scientist/models
GET /api/stock-manager/alerts
GET /api/insights/dashboard
GET /api/admin/system-health
... (3+ more)
```

**New Endpoint** (USE THIS):
```
GET /api/v1/dashboard
```

**Response**:
```json
{
  "role": "MANAGER",
  "managerDashboard": {
    "machineAvailabilityPercent": 95.5,
    "maintenanceCompliancePercent": 88.0,
    "pendingApprovalsCount": 3,
    "criticalAlertsCount": 1,
    "kpis": {
      "mtbf": 720.0,
      "mttr": 4.5,
      "oee": 82.3
    }
  }
}
```

**How It Works**:
1. User calls GET /api/v1/dashboard
2. DashboardController extracts role from JWT
3. Delegates to DashboardService.getDashboardForRole()
4. Service calls read-only query services (MaintenanceQueryService, InventoryAnalyticsService, etc.)
5. Returns role-specific DTO

---

### 3.3 Service Separation Pattern

#### Write Service (Transactional = true)
```java
@Service
@RequiredArgsConstructor
@Transactional  // WRITES enabled
public class PartService {
    public PartResponse createPart(PartRequest request) { }
    public PartResponse updatePart(Long id, PartUpdateRequest request) { }
    // 250 lines max
}
```

#### Read Service (Transactional = readOnly)
```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)  // NO writes
public class InventoryAnalyticsService {
    public InventoryStatsResponse getInventoryStats() { }
    public List<LowStockAlertResponse> getLowStockAlerts() { }
    // Used by Dashboard ONLY
}
```

**Rule**: Dashboard calls ONLY `*QueryService` or `*AnalyticsService`, never write services

---

### 3.4 Inventory Flow Example

**Complete Workflow: Request → Approve → Order → Receive**

```
1. TECHNICIAN uses part in maintenance task
   PartService.updateStockAfterUsage(partId, quantity)
   Part stock decreases; status → LOW_STOCK if below minimum

2. LOW_STOCK triggers reorder request
   ReorderService.requestReorder(partId, quantity)
   ReorderRequest status = REQUESTED

3. MANAGER approves
   ReorderService.approveReorder(reorderId, approved=true)
   ReorderRequest status = APPROVED

4. STOCK_MANAGER creates purchase order
   StockOrderService.createStockOrder(reorderId, supplierPO)
   StockOrder status = PENDING

5. Supplier ships
   StockOrderService.updateStatus(stockOrderId, SHIPPED)

6. STOCK_MANAGER receives delivery
   StockOrderService.recordReceipt(stockOrderId, quantityReceived)
   Part stock increases; status → AVAILABLE
```

---

## 4. DTO Naming Convention

### Old Way (REMOVE)
```
PartDto, PartDtoV2
ManagerPartDto, TechnicianPartDto, AdminPartDto
GetPartResponse, PostPartResponse
```

### New Way (USE)
```
PartRequest       (POST /parts body)
PartResponse      (GET /parts/{id} response)
PartUpdateRequest (PUT /parts/{id} body)
PartMapper        (converts between entity ↔ DTO)
```

**Pattern**:
- `XyzRequest` for POST/PUT bodies
- `XyzResponse` for GET responses
- `XyzMapper` for entity ↔ DTO conversion

---

## 5. Endpoint Count Per Role

### TECHNICIAN (max 5 endpoints)
```
GET    /api/v1/dashboard                    (Dashboard)
GET    /api/v1/maintenance?assignedTo=me    (My tasks)
POST   /api/v1/inventory/usage              (Use a part)
POST   /api/v1/reorder-requests             (Request parts)
DELETE /api/v1/task/{id}                    (Cancel task)
```

### MANAGER (max 7 endpoints)
```
GET    /api/v1/dashboard                    (Dashboard + KPIs)
POST   /api/v1/reorder-requests/{id}/approve
GET    /api/v1/inventory/reorder-requests/pending
POST   /api/v1/maintenance                  (Create maintenance)
GET    /api/v1/metrics/kpis                 (KPI queries)
PUT    /api/v1/task/{id}                    (Update task)
GET    /api/v1/alerts                       (Alert list)
```

### STOCK_MANAGER (max 5 endpoints)
```
GET    /api/v1/dashboard                    (Dashboard)
POST   /api/v1/inventory/parts              (Create part)
GET    /api/v1/inventory/low-stock
POST   /api/v1/stock-orders                 (Create PO)
PUT    /api/v1/stock-orders/{id}/receive    (Receive delivery)
```

### DATA_SCIENTIST (max 5 endpoints)
```
GET    /api/v1/dashboard                    (Dashboard + Model metrics)
POST   /api/v1/predictions/model/train
GET    /api/v1/predictions/latest
PUT    /api/v1/ml-models/{id}/deploy
GET    /api/v1/sensor-data/{machineId}
```

### ADMIN (max 6 endpoints)
```
GET    /api/v1/dashboard                    (Full system status)
POST   /api/v1/users                        (Create user)
GET    /api/v1/users
PUT    /api/v1/users/{id}
DELETE /api/v1/users/{id}
GET    /api/v1/audit-logs
```

### SUPER_ADMIN (max 7 endpoints)
```
GET    /api/v1/dashboard                    (Full system status)
POST   /api/v1/users
GET    /api/v1/users
PUT    /api/v1/users/{id}/role              (Assign role)
DELETE /api/v1/users/{id}
POST   /api/v1/backup/trigger
GET    /api/v1/system-config
```

---

## 6. Security Configuration

### SecurityConfig.java

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable() // JWT doesn't need CSRF
            .authorizeRequests()
                .antMatchers("/api/v1/auth/**").permitAll()
                .antMatchers("/api/v1/**").authenticated()
                .anyRequest().authenticated()
            .and()
            .addFilter(new JwtAuthenticationFilter(...)) // Custom JWT filter
            .sessionManagement().sessionCreationPolicy(STATELESS);
        
        return http.build();
    }
}
```

**Never use**:
- ❌ `@PreAuthorize` on @Service classes
- ❌ Role checks inside business logic
- ❌ Multiple permission constants with same meaning

**Always use**:
- ✅ @PreAuthorize on @RestController methods only
- ✅ PermissionConstants for all permission strings
- ✅ One place to change permissions globally

---

## 7. Entities & Relationships

### Clean Entity Design

```
User (user-module)
  ├─ Permissions defined by role
  ├─ OneToMany: Maintenance (assignedTo)
  └─ OneToMany: Task (assignedTo)

Machine (machine-module)
  ├─ OneToMany: Maintenance (machineId)
  ├─ OneToMany: Task (machineId)
  ├─ OneToMany: Alert (machineId)
  ├─ OneToMany: Prediction (machineId)
  └─ OneToMany: Sensor (machineId)

Part (inventory-module)
  ├─ OneToMany: InventoryUsage (partId)
  ├─ OneToMany: ReorderRequest (partId)
  └─ OneToMany: StockOrder (partId)

Maintenance (maintenance-module)
  └─ OneToMany: Task (maintenanceId)
```

**No Circular Dependencies**: Keep relationships uni-directional when possible

---

## 8. Database Migrations (Flyway)

### File Structure
```
/data-module/resources/db/migration
    V1__init.sql                           (all tables)
    V2__update_users_role_constraint.sql   (migrations)
    V3__add_inventory_workflow.sql         (add fk constraints)
```

### V1__init.sql Example
```sql
-- Users (used by all modules)
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    role VARCHAR(50) NOT NULL,
    status VARCHAR(50) DEFAULT 'ACTIVE',
    created_date TIMESTAMP NOT NULL,
    version BIGINT DEFAULT 0
);

-- Parts (inventory-module)
CREATE TABLE parts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    part_number VARCHAR(100) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    current_stock INT NOT NULL,
    minimum_stock INT NOT NULL,
    status VARCHAR(50) DEFAULT 'AVAILABLE',
    created_date TIMESTAMP NOT NULL,
    version BIGINT DEFAULT 0
);

-- ... other tables
```

---

## 9. Implementation Checklist

### Phase 1: Foundation (Week 1)
- [ ] Copy `PermissionConstants.java` to common-module
- [ ] Create `SecurityConfig.java` with JWT filter
- [ ] Implement `DashboardController` + `DashboardService` + DTOs
- [ ] Test: GET /api/v1/dashboard returns correct role data

### Phase 2: Inventory Refactoring (Week 2)
- [ ] Create /inventory-module with structure above
- [ ] Implement PartService (CRUD, <250 lines)
- [ ] Implement ReorderService (workflow, <250 lines)
- [ ] Implement InventoryAnalyticsService (read-only)
- [ ] Write tests: all 7 endpoints pass
- [ ] Test stock flow: usage → reorder → approval → order → receipt

### Phase 3: Scale to Other Features (Week 2-3)
- [ ] Repeat for Maintenance, Task, Alert, Machine, Prediction, User modules
- [ ] Each module: Entity + Repository + Service + Controller + DTO
- [ ] Keep services <300 lines each

### Phase 4: Delete Old Code (Week 3)
- [ ] Remove 20+ old ManagerInsightsService, TechnicianService, etc.
- [ ] Remove 8+ role-specific controllers
- [ ] Remove 30+ unused DTOs
- [ ] Clean up endpoints

### Phase 5: Verify & Deploy (Week 3)
- [ ] mvn clean install (no compilation errors)
- [ ] mvn test && mvn verify (all tests pass)
- [ ] Load test: 10k requests/sec to /api/v1/dashboard
- [ ] Check permission enforcement: unauthorized requests return 403

---

## 10. Common Pitfalls to Avoid

❌ **Pitfall 1**: Services calling services
```java
// WRONG
public class ManagerDashboardService {
    public ManagerDashboard getDashboard() {
        return new ManagerDashboard(
            maintenanceService.getMetrics(),      // ❌ Service calling service
            inventoryService.getStats(),
            alertService.getAlerts()
        );
    }
}
```

✅ **Correct**:
```java
// RIGHT
public class ManagerDashboard {
    public ManagerDashboard getDashboard() {
        return new ManagerDashboard(
            maintenanceRepository.calculateAvailability(),  // ✅ Direct repository
            inventoryRepository.getTotalValue(),
            alertRepository.countByStatus(CRITICAL)
        );
    }
}
```

---

❌ **Pitfall 2**: Scattered @PreAuthorize
```java
// WRONG
@PostMapping
@PreAuthorize("hasAnyRole('MANAGER','ADMIN','SUPER_ADMIN')")  // ❌ String literal
public void approve() { }
```

✅ **Correct**:
```java
// RIGHT
@PostMapping
@PreAuthorize(PermissionConstants.PERM_REORDER_APPROVE)  // ✅ Constant
public void approve() { }
```

---

❌ **Pitfall 3**: Role-specific DTOs
```java
// WRONG
public class Manager PartDto { }     // ❌ Role-specific
public class TechnicianPartDto { }   // ❌ Role-specific
```

✅ **Correct**:
```java
// RIGHT
public class PartRequest { }    // ✅ Generic
public class PartResponse { }   // ✅ Generic
```

---

❌ **Pitfall 4**: Big services
```java
// WRONG
public class InventoryService {
    // 800 lines mixing:
    // - Part CRUD
    // - Reorder workflow
    // - Analytics
    // - Stock orders
    // - Notifications
}
```

✅ **Correct**:
```java
// RIGHT
public class PartService { }                        // 150 lines
public class ReorderService { }                     // 120 lines
public class InventoryAnalyticsService { }          // 80 lines (read-only)
public class StockOrderService { }                  // 140 lines
```

---

## 11. Verification Checklist

Before going to production:

- [ ] All controllers use `PermissionConstants` (grep for @PreAuthorize to verify)
- [ ] No service calls another service (only calls repositories)
- [ ] All services <300 lines
- [ ] All DTOs follow XRequest/XResponse naming
- [ ] Dashboard endpoint returns role-specific data correctly
- [ ] All role-specific controllers deleted
- [ ] All role-specific services deleted
- [ ] No circular entity dependencies
- [ ] Flyway migrations applied cleanly
- [ ] 100% test coverage for critical paths
- [ ] Load test: 10k req/sec sustained for 5 minutes
- [ ] Permission matrix verified: every role tested for 403 on unauthorized endpoints

---

## 12. Post-Refactoring Maintenance

### Adding a New Endpoint

Example: Add "get machine efficiency" endpoint

1. **Add permission** to PermissionConstants:
```java
public static final String PERM_MACHINE_EFFICIENCY_READ = 
    "hasAnyRole('MANAGER', 'DATA_SCIENTIST', 'ADMIN', 'SUPER_ADMIN')";
```

2. **Add endpoint** to MachineController:
```java
@GetMapping("/{id}/efficiency")
@PreAuthorize(PermissionConstants.PERM_MACHINE_EFFICIENCY_READ)
public ResponseEntity<MachineEfficiencyResponse> getMachineEfficiency(@PathVariable Long id) {
    return ResponseEntity.ok(machineQueryService.getEfficiency(id));
}
```

3. **Add query** to MachineQueryService:
```java
public MachineEfficiencyResponse getEfficiency(Long machineId) {
    // Calculate from historical data
}
```

**Total changes**: 3 files. No architectural changes needed.

---

## 13. Summary

| Aspect | Before | After |
|--------|--------|-------|
| **Controllers** | 19 role-specific | 7 feature-based |
| **Services** | 20+ (1000s lines total) | ~15 (<300 lines each) |
| **DTOs** | 50+ (role-specific) | ~20 (generic Request/Response) |
| **Permissions** | Scattered in 50+ places | 1 PermissionConstants.java |
| **Dashboard Endpoints** | 8 different URIs | 1 unified /api/v1/dashboard |
| **Circular Dependencies** | Multiple | None |
| **Testability** | Hard (tight coupling) | Easy (single responsibility) |
| **Maintenance** | High (change permission = 5+ places) | Low (change once) |

---

**You're ready to refactor! Start with Phase 1 (PermissionConstants + Dashboard), then scale to all modules. Good luck!**
