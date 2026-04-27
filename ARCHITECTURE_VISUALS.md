# Architecture Visuals & Diagrams

## 1. Clean Architecture Request Flow

```
┌─────────────────────────────────────────────────────────────────────┐
│                          CLIENT LAYER                               │
│                      (React/Angular Frontend)                        │
└────────────────────────────────┬──────────────────────────────────┘
                                 │
                                 ▼
                    GET /api/v1/dashboard
                Bearer <JWT_TOKEN>
                        │
        ┌───────────────┼───────────────┐
        │               │               │
        ▼               ▼               ▼
   ┌──────────┐   ┌──────────┐   ┌──────────┐
   │DashboardC│   │Inventory │   │Maintenance
   │ontroller │   │Controller│   │Controller│
   └────┬─────┘   └────┬─────┘   └────┬─────┘
        │              │              │
        ├─ Extract Role from JWT
        ├─ @PreAuthorize(PERM_DASHBOARD_READ)  ← PermissionConstants!
        │
        ▼
   ┌─────────────────────────────────────────┐
   │      DashboardService (read-only)       │
   │  @Transactional(readOnly=true)          │
   └───────┬─────────────────────────────────┘
           │
           ├─ if role == MANAGER
           │    getDashboardForRole("ROLE_MANAGER", userId)
           │       ├─→ MaintenanceQueryService.calculateAvailability()
           │       ├─→ InventoryAnalyticsService.getPendingReorders()
           │       ├─→ AlertQueryService.countCriticalAlerts()
           │       └─→ MachineQueryService.getTopFailingMachines()
           │
           ├─ if role == TECHNICIAN
           │    getDashboardForRole("ROLE_TECHNICIAN", userId)
           │       ├─→ TaskQueryService.getAssignedTasks(userId)
           │       ├─→ AlertQueryService.countActiveAlerts()
           │       └─→ InventoryAnalyticsService.countLowStockParts()
           │
           └─ if role == STOCK_MANAGER
                getDashboardForRole("ROLE_STOCK_MANAGER", userId)
                   ├─→ InventoryAnalyticsService.getStats()
                   ├─→ InventoryAnalyticsService.getLowStockAlerts()
                   └─→ InventoryAnalyticsService.getPendingOrders()
           
           All Query Services call repositories directly (NO service calls!)
           ▼
        ┌────────────────────────────────────┐
        │ Repository Layer                   │
        │ (Spring Data JPA)                  │
        ├────────────────────────────────────┤
        │ MaintenanceRepository              │
        │ InventoryRepository                │
        │ AlertRepository                    │
        │ ... (others)                       │
        └────────────┬───────────────────────┘
                     │
                     ▼
        ┌────────────────────────────────────┐
        │   PostgreSQL / MySQL Database      │
        └────────────────────────────────────┘
                     ▲
                     │
                Response JSON
         (DashboardResponse with role-specific DTO)
```

---

## 2. Feature Module Structure

```
inventory-module (EXAMPLE)
│
├── src/main/java/com/pfe/predictive/inventory/
│   │
│   ├── entity/
│   │   ├── Part.java                    (CRUD entity)
│   │   ├── InventoryUsage.java          (Usage tracking)
│   │   ├── ReorderRequest.java          (Request workflow)
│   │   └── StockOrder.java              (Purchase order)
│   │
│   ├── repository/
│   │   ├── PartRepository.java
│   │   ├── InventoryUsageRepository.java
│   │   ├── ReorderRequestRepository.java
│   │   └── StockOrderRepository.java
│   │
│   ├── service/
│   │   ├── PartService.java                     (write, <250 lines)
│   │   │   ├── createPart(Request) → Response
│   │   │   ├── updatePart(id, Request) → Response
│   │   │   ├── deletePart(id)
│   │   │   └── updateStockAfterXxx()
│   │   │
│   │   ├── ReorderService.java                  (write, <250 lines)
│   │   │   ├── requestReorder(Request) → Response
│   │   │   ├── approveReorder(id, approved)
│   │   │   └── getPendingReorders() → List
│   │   │
│   │   └── InventoryAnalyticsService.java       (read-only, <250 lines)
│   │       ├── getInventoryStats() → Response
│   │       ├── getLowStockAlerts() → List
│   │       ├── getCriticalReorders() → List
│   │       └── countTotalParts() → long
│   │
│   ├── mapper/
│   │   ├── PartMapper.java
│   │   ├── ReorderMapper.java
│   │   └── StockOrderMapper.java
│   │
│   ├── dto/
│   │   ├── PartRequest.java             (POST body)
│   │   ├── PartResponse.java            (GET response)
│   │   ├── PartUpdateRequest.java       (PUT body)
│   │   ├── ReorderRequestRequest.java
│   │   ├── ReorderRequestResponse.java
│   │   └── InventoryStatsResponse.java
│   │
│   └── controller/
│       └── InventoryController.java
│           ├── POST   /api/v1/inventory/parts
│           ├── GET    /api/v1/inventory/parts
│           ├── GET    /api/v1/inventory/parts/{id}
│           ├── PUT    /api/v1/inventory/parts/{id}
│           ├── DELETE /api/v1/inventory/parts/{id}
│           ├── POST   /api/v1/inventory/reorder-requests
│           ├── POST   /api/v1/inventory/reorder-requests/{id}/approve
│           ├── GET    /api/v1/inventory/reorder-requests/pending
│           └── GET    /api/v1/inventory/stats
│
└── src/main/resources/
    ├── db/migration/
    │   └── V3__inventory.sql
    └── application-inventory.yml (optional config)
```

---

## 3. Complete Workflow: Inventory Reorder Example

```
┌─────────────────────────────────────────────────────────────────────┐
│                    REORDER WORKFLOW                                 │
└─────────────────────────────────────────────────────────────────────┘

1️⃣  TECHNICIAN USES PART IN MAINTENANCE
    ┌─────────────────────────────────┐
    │ POST /api/v1/inventory/usage    │
    │ {                               │
    │   partId: 5,                    │
    │   quantityUsed: 2,              │
    │   taskId: 10                    │
    │ }                               │
    └──────────┬──────────────────────┘
               │
               ▼
    PartService.updateStockAfterUsage(5, 2)
    │
    ├─ part.currentStock = 8 - 2 = 6
    ├─ 6 <= minimumStock(10)?  YES
    │
    └─ part.status = LOW_STOCK  ⚠️


2️⃣  LOW STOCK DETECTED → REORDER REQUESTED
    ┌──────────────────────────────────────┐
    │ POST /api/v1/inventory/reorder-requests
    │ {                                    │
    │   partId: 5,                         │
    │   quantity: 20,                      │
    │   reason: "LOW_STOCK"                │
    │ }                                    │
    └────────────┬─────────────────────────┘
                 │
                 ▼
    ReorderService.requestReorder()
    │
    └─ reorderRequest.status = REQUESTED  ⏳


3️⃣  MANAGER APPROVES REORDER
    ┌─────────────────────────────────────────┐
    │ POST /api/v1/reorder-requests/10/approve
    │ @PreAuthorize(PERM_REORDER_APPROVE) ✅ │
    │ {                                       │
    │   approved: true,                       │
    │   reason: null                          │
    │ }                                       │
    └──────────┬────────────────────────────┘
               │
               ▼
    ReorderService.approveReorder(#10, approved=true)
    │
    ├─ reorderRequest.status = APPROVED  ✅
    └─ reorderRequest.approvedBy = "manager"


4️⃣  STOCK MANAGER CREATES PURCHASE ORDER
    ┌───────────────────────────────────────┐
    │ POST /api/v1/stock-order             │
    │ {                                     │
    │   reorderRequestId: 10,               │
    │   supplierPurchaseOrder: "PO-2024-1" │
    │   expectedDeliveryDate: "2024-02-05" │
    │ }                                     │
    │ @PreAuthorize(PERM_STOCK_MANAGE) ✅   │
    │                                       │
    └────────────┬──────────────────────────┘
                 │
                 ▼
    StockOrderService.createStockOrder()
    │
    ├─ stockOrder.status = PENDING
    ├─ stockOrder.quantity = 20
    └─ stockOrder.cost = 20 * 150 = 3000


5️⃣  SUPPLIER SHIPS
    ┌────────────────────────────┐
    │ System receives notification
    │ or manual update            │
    └────────┬───────────────────┘
             │
             ▼
    StockOrderService.updateStatus(orderId, SHIPPED)
    │
    └─ stockOrder.status = SHIPPED  🚚


6️⃣  DELIVERY RECEIVED
    ┌────────────────────────────────────────┐
    │ PUT /api/v1/stock-order/15/receive     │
    │ {                                      │
    │   quantityReceived: 20,                │
    │   proofOfDelivery: "receipt-img-url"   │
    │ }                                      │
    │ @PreAuthorize(PERM_STOCK_MANAGE) ✅    │
    └────────┬─────────────────────────────┘
             │
             ▼
    StockOrderService.recordReceipt(15, 20)
    │
    ├─ InventoryUsageService.recordReceipt()
    │
    ├─ part = partRepository.findById(5)
    ├─ part.currentStock = 6 + 20 = 26
    ├─ part.status = AVAILABLE  ✅
    │
    └─ stockOrder.status = DELIVERED  📦
       stockOrder.deliveredDate = NOW

WORKFLOW COMPLETE ✅
Bearing stock restored from 6 → 26 units
```

---

## 4. Permission Model Comparison

### OLD (CHAOTIC)
```
@RestController
@RequestMapping("/api/manager-insights")
public class ManagerInsightsController {

    @PreAuthorize("hasAnyRole('MANAGER','ADMIN','SUPER_ADMIN')")  ← Hardcoded
    @GetMapping("/kpis")
    public Response getKpis() { }

    @PreAuthorize("hasAnyRole('MANAGER','ADMIN','SUPER_ADMIN')")  ← Same string!
    @PostMapping("/approve-maintenance")
    public Response approveMaintenance() { }

    @PreAuthorize("hasAnyRole('MANAGER','ADMIN','SUPER_ADMIN')")  ← Duplicated!
    @PutMapping("/update-budget")
    public Response updateBudget() { }
}

// To change permission: Replace in 50+ places = ERROR PRONE! 🔴
```

### NEW (CLEAN)
```
@RestController
@RequestMapping("/api/v1")
public class DashboardController {

    @PreAuthorize(PermissionConstants.PERM_DASHBOARD_READ)  ← Named constant
    @GetMapping("/dashboard")
    public Response getDashboard() { }
}

// PermissionConstants.java (ONE PLACE)
public class PermissionConstants {
    public static final String PERM_DASHBOARD_READ = 
        "hasAnyRole('TECHNICIAN','MANAGER','STOCK_MANAGER',"    // Easy to read
        "'DATA_SCIENTIST','ADMIN','SUPER_ADMIN')";              // Easy to change

    // To change permission: 1 file, 1 line = SAFE & AUDITABLE! ✅
}
```

---

## 5. Service Architecture Principle: Single Responsibility

```
┌────────────────────────────────────────────────────────────────┐
│                  OLD: God Services ❌                          │
└────────────────────────────────────────────────────────────────┘

InventoryService.java  (800+ lines)
├── CRUD Operations
│   ├── createPart()
│   ├── updatePart()
│   ├── deletePart()
│   └── getPart()
│
├── Reorder Workflow
│   ├── requestReorder()
│   ├── approveReorder()
│   └── getReorderStatus()
│
├── Stock Orders
│   ├── createStockOrder()
│   ├── updateStockOrder()
│   └── receiveOrder()
│
├── Analytics
│   ├── getInventoryStats()
│   ├── getLowStockAlerts()
│   └── calculateTurnover()
│
├── Notifications
│   ├── notifyLowStock()
│   └── notifyReorderApproved()
│
└── Calling Other Services ❌
    ├── machineService.updateStatus()
    ├── alertService.createAlert()
    └── userService.notifyUser()

Problems:
- Hard to test (too many dependencies)
- Hard to change (1 change breaks multiple features)
- Hard to understand (mixed concerns)
- Tight coupling (calling other services)
```

```
┌────────────────────────────────────────────────────────────────┐
│               NEW: Focused Services ✅                         │
└────────────────────────────────────────────────────────────────┘

PartService.java  (≈150 lines)          ✅ Single file
├── createPart()
├── updatePart()
├── deletePart()
├── getPart()
├── getLowStockParts()
└── updateStockAfterUsage()
    └── Calls: PartRepository ONLY ✅

ReorderService.java  (≈120 lines)       ✅ Single file
├── requestReorder()
├── approveReorder()
├── getPendingReorders()
└── getReorderById()
    └── Calls: ReorderRequestRepository ONLY ✅

InventoryAnalyticsService.java  (≈80 lines)  ✅ Read-only!
├── getInventoryStats()
├── getLowStockAlerts()
├── getCriticalReorders()
├── countTotalParts()
├── countLowStockParts()
└── getStockTurnoverRate()
    └── Calls: Repositories ONLY ✅
    └── @Transactional(readOnly=true) ✅

StockOrderService.java  (≈140 lines)    ✅ Single file
├── createStockOrder()
├── updateStatus()
├── recordReceipt()
└── getOpenOrders()
    └── Calls: Repositories ONLY ✅

Benefits:
- Easy to test (mocked dependencies clear)
- Easy to change (changes isolated)
- Easy to understand (one purpose per class)
- No circular dependencies (no service calls)
```

---

## 6. DTO Naming Convention

```
┌─────────────────────────────────────────────────┐
│            OLD: Role-Specific ❌                │
└─────────────────────────────────────────────────┘

PartDto
ManagerPartDto
TechnicianPartDto
AdminPartDto
PartDtoV2
PartDtoLegacy

GetPartResponse
GetPartByIdResponse
GetAllPartsResponse
CreatePartResponse
UpdatePartResponse

Problems: 15+ naming variants, unclear differences


┌──────────────────────────────────────────────────────┐
│          NEW: Generic Request/Response ✅           │
└──────────────────────────────────────────────────────┘

PartRequest       ← For POST, PUT bodies
PartResponse      ← For GET responses
PartUpdateRequest ← For PUT bodies (when different from create)

→ Created by Controller from incoming JSON
→ Returned by Controller in response body
→ Mapped to/from Entity via Mapper

Benefits: Predictable naming, fewer files, clear semantics
```

---

## 7. Role Hierarchy & Dashboard Response

```
                   ROLE HIERARCHY
                        
                    SUPER_ADMIN
                         ↑
                       ADMIN
                    /        \
                MANAGER      (Administrative roles)
              /        \
      TECHNICIAN   STOCK_MANAGER
           |              |
      Operational      Operational
      (Field Work)    (Inventory)
           
           
                   DATA_SCIENTIST
                   (Independent)
                   
      
┌──────────────────────────────────────────────────────────────┐
│    Dashboard Response varies by Role                         │
└──────────────────────────────────────────────────────────────┘

TechnicianDashboardResponse:
├── assignedTasksCount: 3
├── completedTodayCount: 1
├── activeAlertsCount: 2
├── lowStockPartsCount: 5
├── recentTasks: [...]
└── upcomingMaintenance: [...]

ManagerDashboardResponse:
├── machineAvailabilityPercent: 95.5%
├── maintenanceCompliancePercent: 88.0%
├── pendingApprovalsCount: 3
├── criticalAlertsCount: 1
├── topFailingMachines: [...]
├── emergencyReorders: [...]
└── kpis:
    ├── mtbf: 720 hours
    ├── mttr: 4.5 hours
    └── oee: 82.3%

StockManagerDashboardResponse:
├── totalPartsTracked: 250
├── lowStockCount: 15
├── pendingOrdersCount: 8
├── stockTurnoverRate: 4.2x/year
├── criticalReorders: [...]
└── pendingReorderRequests: [...]

DataScientistDashboardResponse:
├── overallModelAccuracyPercent: 94.2%
├── deployedModelsCount: 3
├── trainingJobsRunningCount: 1
├── modelPerformance: [...]
└── recentPredictions: [...]

AdminDashboardResponse:
├── activeUsersCount: 45
├── systemHealth: "OPERATIONAL"
└── lastBackupTime: "2024-01-15T10:00:00Z"

SuperAdminDashboardResponse:
├── systemStatus: "HEALTHY"
├── totalMachinesMonitored: 120
└── uptime: "99.9%"

ONE ENDPOINT, SIX DIFFERENT RESPONSES ✅
```

---

## 8. Technology Stack

```
┌─────────────────────────────────────────┐
│     SPRING BOOT ARCHITECTURE            │
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│         REST API Layer (HTTP)           │
│  @RestController, Spring MVC            │
└────────────┬────────────────────────────┘
             │
┌────────────▼────────────────────────────┐
│    Security Layer                       │
│  JWT + Spring Security                  │
│  @PreAuthorize, SecurityContext         │
└────────────┬────────────────────────────┘
             │
┌────────────▼────────────────────────────┐
│    Service Layer                        │
│  @Service, @Transactional               │
│  Business Logic, Workflows              │
└────────────┬────────────────────────────┘
             │
┌────────────▼────────────────────────────┐
│    Repository Layer                     │
│  Spring Data JPA                        │
│  @Repository, CrudRepository            │
└────────────┬────────────────────────────┘
             │
┌────────────▼────────────────────────────┐
│    Database                             │
│  PostgreSQL / MySQL                     │
│  Entities, JPA, Hibernate              │
│  Flyway Migrations                      │
└─────────────────────────────────────────┘

Dependencies:
├── spring-boot-starter-web              (MVC, REST)
├── spring-boot-starter-security         (JWT, Auth)
├── spring-boot-starter-data-jpa         (JPA)
├── spring-boot-starter-validation       (Bean Validation)
├── jjwt                                 (JWT library)
├── lombok                               (Annotations)
├── flyway-core                          (Migrations)
├── postgresql-driver    (or mysql8)     (Database)
└── junit5              (Tests)           (Testing)
```

---

## 9. Deployment Architecture

```
┌──────────────────────────────────────────────────┐
│           PRODUCTION DEPLOYMENT                  │
└──────────────────────────────────────────────────┘

                    LOAD BALANCER (nginx)
                            │
        ┌───────────────────┼───────────────────┐
        │                   │                   │
        ▼                   ▼                   ▼
    ┌─────────┐         ┌─────────┐        ┌─────────┐
    │ Spring  │         │ Spring  │        │ Spring  │
    │ Boot 1  │         │ Boot 2  │        │ Boot 3  │
    │ Port    │         │ Port    │        │ Port    │
    │ :8080   │         │ :8081   │        │ :8082   │
    └────┬────┘         └────┬────┘        └────┬────┘
         │                   │                   │
         └───────────────────┼───────────────────┘
                             │
        ┌────────────────────┴────────────────────┐
        │   Connection Pooling (HikariCP)         │
        └──────────────────┬─────────────────────┘
                           │
        ┌──────────────────▼──────────────────┐
        │   PostgreSQL / MySQL (Master)        │
        │   ├── All Tables (from Flyway)       │
        │   ├── Parts, Machines, Maintenance   │
        │   ├── Users, Predictions, Tasks      │
        │   └── Indexes & Constraints          │
        └───────────────────────────────────────┘

Scaling:
- Horizontal: Add more Spring Boot instances
- Vertical: Increase DB connection pool
- Read Replicas: For analytics (optional)
```

---

## 10. Implementation Sequence (Visual)

```
WEEK 1: FOUNDATION
┌───────────────────────┐
│ Day 1: PermissionConstants, JWT Config │ 
├───────────────────────┤
│ ✅ SecurityConfig.java
│ ✅ JwtTokenProvider.java
│ ✅ PermissionConstants.java
└───────────────────────┘
        │ (Deploy)
        ▼
┌───────────────────────┐
│ Day 2: Verify JWT Works
│ ✅ Generate token
│ ✅ Post to protected endpoint
└───────────────────────┘

WEEK 1: DASHBOARD  
┌───────────────────────┐
│ Day 3: Dashboard Endpoint │
├───────────────────────┤
│ ✅ DashboardController
│ ✅ DashboardService
│ ✅ All role DTOs
└───────────────────────┘
        │ (Deploy)
        ▼
┌───────────────────────┐
│ ✅ Dashboard returns role-specific data
└───────────────────────┘

WEEK 1-2: INVENTORY FEATURE
┌───────────────────────┐
│ Days 4-5: Complete Inventory Module │
├───────────────────────┤
│ ✅ Entities
│ ✅ Repositories
│ ✅ Services (Part, Reorder, Analytics)
│ ✅ DTOs
│ ✅ Mappers
│ ✅ Controller (7 endpoints)
│ ✅ Flyway migration
└───────────────────────┘
        │ (Deploy)
        ▼
┌───────────────────────┐
│ ✅ All 7 endpoints working
│ ✅ Stock flow working
└───────────────────────┘

WEEK 2: SCALE OTHER MODULES
┌───────────────────────────────┐
│ Days 6-8: Maintenance + Others │
├───────────────────────────────┤
│ ✅ Maintenance (repeat pattern)
│ ✅ Task (repeat pattern)
│ ✅ Alert (repeat pattern)
│ ✅ Machine (repeat pattern)
│ ✅ Prediction (repeat pattern)
│ ✅ User (repeat pattern)
└───────────────────────────────┘
        │ (Deploy)
        ▼
┌───────────────────────────────┐
│ ✅ All 6 modules fully functional
└───────────────────────────────┘

WEEK 3: CLEANUP
┌───────────────────────────────┐
│ Days 9-10: Delete Old Code │
├───────────────────────────────┤
│ ✅ Delete role-specific controllers
│ ✅ Delete old services
│ ✅ Delete old DTOs
│ ✅ Fix imports
└───────────────────────────────┘
        │ (Compile)
        ▼
┌───────────────────────────────┐
│ ✅ mvn clean compile (0 errors)
└───────────────────────────────┘

WEEK 3: VALIDATE
┌───────────────────────────────┐
│ Days 11-12: Testing → Deploy │
├───────────────────────────────┤
│ ✅ Unit tests pass
│ ✅ Integration tests pass
│ ✅ Load test (10k req/sec)
│ ✅ Permission matrix verified
│ ✅ Deploy to production
└───────────────────────────────┘
        │
        ▼
   🚀 LIVE!
```

---

**All diagrams reference architecture principles in REFACTORING_GUIDE.md**
