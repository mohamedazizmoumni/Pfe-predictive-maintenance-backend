/**
 * Finance Manager Service
 * Complete Angular service for Finance Manager CRUD operations
 * 
 * Usage:
 * constructor(private financeService: FinanceManagerService) {}
 * 
 * this.financeService.getAllBudgets().subscribe(
 *   budgets => console.log(budgets),
 *   error => console.error(error)
 * );
 */

import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

// ============ INTERFACES ============

export interface Department {
  id: number;
  name: string;
  description: string;
  managerId: number;
  managerName: string;
  budgetYear: number;
  status: string;
  createdDate: string;
  lastModifiedDate: string;
}

export interface Budget {
  id: number;
  departmentId: number;
  departmentName: string;
  fiscalYear: number;
  allocatedAmount: number;
  spentAmount: number;
  remainingAmount: number;
  utilizationPercentage: number;
  isOverBudget: boolean;
  isNearLimit: boolean;
  status: string;
  createdDate: string;
  lastModifiedDate: string;
}

export interface BudgetRequest {
  departmentId: number;
  fiscalYear: number;
  allocatedAmount: number;
}

export interface MaintenanceRapport {
  id: number;
  taskId: number;
  machineId: number;
  technicianId: number;
  technicianName: string;
  status: 'DRAFT' | 'PENDING_MANAGER_APPROVAL' | 'PENDING_FINANCE_APPROVAL' | 'APPROVED' | 'REJECTED';
  laborCost: number;
  totalCost: number;
  parts: RapportPart[];
  description: string;
  notes: string;
  managerApprovedDate?: string;
  financeApprovedDate?: string;
  createdDate: string;
}

export interface RapportPart {
  id: number;
  partId: number;
  partName: string;
  quantity: number;
  unitCost: number;
  totalCost: number;
  description: string;
}

export interface ApprovalRequest {
  notes: string;
}

export interface StockItem {
  id: number;
  name: string;
  partNumber: string;
  description: string;
  quantity: number;
  minimumQuantity: number;
  maximumQuantity: number;
  unitCost: number;
  totalValue: number;
  supplier: string;
  isBelowMinimum: boolean;
  isAboveMaximum: boolean;
  createdDate: string;
}

export interface StockItemRequest {
  name: string;
  partNumber: string;
  description: string;
  quantity: number;
  minimumQuantity: number;
  maximumQuantity: number;
  unitCost: number;
  supplier: string;
}

export interface StockReorderRequest {
  stockItemId: number;
  quantityRequested: number;
  expectedDeliveryDate: string;
}

export interface FinanceDashboard {
  totalBudgetAllocated: number;
  totalBudgetSpent: number;
  totalBudgetRemaining: number;
  overallUtilizationPercentage: number;
  departmentCount: number;
  budgetAlerts: BudgetAlert[];
  topSpendingDepartments: TopSpendingDepartment[];
  lastUpdated: string;
}

export interface BudgetAlert {
  departmentId: number;
  departmentName: string;
  utilizationPercentage: number;
  alertType: 'NEAR_LIMIT' | 'OVER_BUDGET';
}

export interface TopSpendingDepartment {
  departmentId: number;
  departmentName: string;
  spentAmount: number;
  allocatedAmount: number;
  utilizationPercentage: number;
}

export interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  currentPage: number;
  pageSize: number;
}

// ============ SERVICE ============

@Injectable({
  providedIn: 'root'
})
export class FinanceManagerService {
  private apiUrl = 'http://localhost:8080/api/v1/finance';

  constructor(private http: HttpClient) {}

  // ============ DEPARTMENT ENDPOINTS ============

  /**
   * Get all departments
   * GET /api/v1/finance/departments
   */
  getAllDepartments(): Observable<Department[]> {
    return this.http.get<Department[]>(`${this.apiUrl}/departments`);
  }

  /**
   * Get active departments only
   * GET /api/v1/finance/departments/active
   */
  getActiveDepartments(): Observable<Department[]> {
    return this.http.get<Department[]>(`${this.apiUrl}/departments/active`);
  }

  /**
   * Get department by ID
   * GET /api/v1/finance/departments/{id}
   */
  getDepartmentById(id: number): Observable<Department> {
    return this.http.get<Department>(`${this.apiUrl}/departments/${id}`);
  }

  /**
   * Search departments by name or description
   * GET /api/v1/finance/departments/search?q={query}
   */
  searchDepartments(query: string): Observable<Department[]> {
    const params = new HttpParams().set('q', query);
    return this.http.get<Department[]>(`${this.apiUrl}/departments/search`, { params });
  }

  // ============ BUDGET ENDPOINTS ============

  /**
   * Get all budgets
   * GET /api/v1/finance/department-budgets
   */
  getAllBudgets(): Observable<Budget[]> {
    return this.http.get<Budget[]>(`${this.apiUrl}/department-budgets`);
  }

  /**
   * Get budget by ID
   * GET /api/v1/finance/department-budgets/{id}
   */
  getBudgetById(id: number): Observable<Budget> {
    return this.http.get<Budget>(`${this.apiUrl}/department-budgets/${id}`);
  }

  /**
   * Create new budget
   * POST /api/v1/finance/department-budgets
   */
  createBudget(request: BudgetRequest): Observable<Budget> {
    return this.http.post<Budget>(`${this.apiUrl}/department-budgets`, request);
  }

  /**
   * Update budget
   * PUT /api/v1/finance/department-budgets/{id}
   */
  updateBudget(id: number, request: BudgetRequest): Observable<Budget> {
    return this.http.put<Budget>(`${this.apiUrl}/department-budgets/${id}`, request);
  }

  /**
   * Get budgets by fiscal year
   * GET /api/v1/finance/department-budgets/year/{fiscalYear}
   */
  getBudgetsByYear(fiscalYear: number): Observable<Budget[]> {
    return this.http.get<Budget[]>(`${this.apiUrl}/department-budgets/year/${fiscalYear}`);
  }

  /**
   * Get budgets by department
   * GET /api/v1/finance/department-budgets/department/{departmentId}
   */
  getBudgetsByDepartment(departmentId: number): Observable<Budget[]> {
    return this.http.get<Budget[]>(`${this.apiUrl}/department-budgets/department/${departmentId}`);
  }

  /**
   * Get budgets near limit (utilization >= 70%)
   * GET /api/v1/finance/department-budgets/alerts/near-limit
   */
  getBudgetsNearLimit(): Observable<Budget[]> {
    return this.http.get<Budget[]>(`${this.apiUrl}/department-budgets/alerts/near-limit`);
  }

  /**
   * Get over-budget budgets (utilization > 100%)
   * GET /api/v1/finance/department-budgets/alerts/over-budget
   */
  getOverBudgetBudgets(): Observable<Budget[]> {
    return this.http.get<Budget[]>(`${this.apiUrl}/department-budgets/alerts/over-budget`);
  }

  // ============ MAINTENANCE RAPPORT ENDPOINTS ============

  /**
   * Get all rapports with optional filtering and pagination
   * GET /api/v1/finance/rapports?status={status}&page={page}&size={size}
   */
  getRapports(
    status?: string,
    page: number = 0,
    size: number = 20
  ): Observable<PaginatedResponse<MaintenanceRapport>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    
    if (status) {
      params = params.set('status', status);
    }
    
    return this.http.get<PaginatedResponse<MaintenanceRapport>>(
      `${this.apiUrl}/rapports`,
      { params }
    );
  }

  /**
   * Get rapport by ID
   * GET /api/v1/finance/rapports/{id}
   */
  getRapportById(id: number): Observable<MaintenanceRapport> {
    return this.http.get<MaintenanceRapport>(`${this.apiUrl}/rapports/${id}`);
  }

  /**
   * Get pending finance approvals
   * GET /api/v1/finance/rapports/pending/finance
   */
  getPendingFinanceApprovals(): Observable<MaintenanceRapport[]> {
    return this.http.get<MaintenanceRapport[]>(`${this.apiUrl}/rapports/pending/finance`);
  }

  /**
   * Approve rapport by finance manager
   * POST /api/v1/finance/rapports/{id}/approve-finance
   */
  approveRapportByFinance(id: number, notes: string): Observable<MaintenanceRapport> {
    const request: ApprovalRequest = { notes };
    return this.http.post<MaintenanceRapport>(
      `${this.apiUrl}/rapports/${id}/approve-finance`,
      request
    );
  }

  /**
   * Reject rapport by finance manager
   * POST /api/v1/finance/rapports/{id}/reject-finance
   */
  rejectRapportByFinance(id: number, notes: string): Observable<MaintenanceRapport> {
    const request: ApprovalRequest = { notes };
    return this.http.post<MaintenanceRapport>(
      `${this.apiUrl}/rapports/${id}/reject-finance`,
      request
    );
  }

  // ============ STOCK ITEM ENDPOINTS ============

  /**
   * Get all stock items
   * GET /api/v1/finance/stock/items
   */
  getStockItems(): Observable<StockItem[]> {
    return this.http.get<StockItem[]>(`${this.apiUrl}/stock/items`);
  }

  /**
   * Get stock item by ID
   * GET /api/v1/finance/stock/items/{id}
   */
  getStockItemById(id: number): Observable<StockItem> {
    return this.http.get<StockItem>(`${this.apiUrl}/stock/items/${id}`);
  }

  /**
   * Create new stock item
   * POST /api/v1/finance/stock/items
   */
  createStockItem(request: StockItemRequest): Observable<StockItem> {
    return this.http.post<StockItem>(`${this.apiUrl}/stock/items`, request);
  }

  /**
   * Update stock item quantity
   * PUT /api/v1/finance/stock/items/{id}/quantity
   */
  updateStockQuantity(id: number, quantity: number): Observable<StockItem> {
    return this.http.put<StockItem>(
      `${this.apiUrl}/stock/items/${id}/quantity`,
      { quantity }
    );
  }

  /**
   * Get stock items below minimum threshold
   * GET /api/v1/finance/stock/items/alerts/below-minimum
   */
  getStockItemsBelowMinimum(): Observable<StockItem[]> {
    return this.http.get<StockItem[]>(`${this.apiUrl}/stock/items/alerts/below-minimum`);
  }

  /**
   * Get total stock value
   * GET /api/v1/finance/stock/items/value/total
   */
  getTotalStockValue(): Observable<{ totalValue: number; itemCount: number; lastUpdated: string }> {
    return this.http.get<{ totalValue: number; itemCount: number; lastUpdated: string }>(
      `${this.apiUrl}/stock/items/value/total`
    );
  }

  // ============ STOCK REORDER ENDPOINTS ============

  /**
   * Request stock reorder
   * POST /api/v1/finance/stock/reorders
   */
  requestReorder(request: StockReorderRequest): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/stock/reorders`, request);
  }

  /**
   * Approve stock reorder
   * POST /api/v1/finance/stock/reorders/{id}/approve
   */
  approveReorder(id: number, expectedDeliveryDate: string): Observable<any> {
    return this.http.post<any>(
      `${this.apiUrl}/stock/reorders/${id}/approve`,
      { expectedDeliveryDate }
    );
  }

  /**
   * Receive stock reorder
   * POST /api/v1/finance/stock/reorders/{id}/receive
   */
  receiveReorder(id: number, quantityReceived: number, receivedDate: string): Observable<any> {
    return this.http.post<any>(
      `${this.apiUrl}/stock/reorders/${id}/receive`,
      { quantityReceived, receivedDate }
    );
  }

  /**
   * Get pending reorders
   * GET /api/v1/finance/stock/reorders/pending
   */
  getPendingReorders(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/stock/reorders/pending`);
  }

  /**
   * Get approved but not received reorders
   * GET /api/v1/finance/stock/reorders/approved-not-received
   */
  getApprovedNotReceivedReorders(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/stock/reorders/approved-not-received`);
  }

  // ============ DASHBOARD ENDPOINTS ============

  /**
   * Get finance dashboard
   * GET /api/v1/finance/department-dashboard
   */
  getFinanceDashboard(): Observable<FinanceDashboard> {
    return this.http.get<FinanceDashboard>(`${this.apiUrl}/department-dashboard`);
  }

  /**
   * Get top spending departments
   * GET /api/v1/finance/department-dashboard/top-spending?limit={limit}
   */
  getTopSpendingDepartments(limit: number = 5): Observable<TopSpendingDepartment[]> {
    const params = new HttpParams().set('limit', limit.toString());
    return this.http.get<TopSpendingDepartment[]>(
      `${this.apiUrl}/department-dashboard/top-spending`,
      { params }
    );
  }
}

// ============ USAGE EXAMPLES ============

/**
 * Example Component Usage
 */

/*
import { Component, OnInit } from '@angular/core';
import { FinanceManagerService, Budget, MaintenanceRapport } from './finance-manager.service';

@Component({
  selector: 'app-finance-manager',
  templateUrl: './finance-manager.component.html',
  styleUrls: ['./finance-manager.component.css']
})
export class FinanceManagerComponent implements OnInit {
  budgets: Budget[] = [];
  pendingRapports: MaintenanceRapport[] = [];
  loading = false;
  error: string | null = null;

  constructor(private financeService: FinanceManagerService) {}

  ngOnInit(): void {
    this.loadBudgets();
    this.loadPendingRapports();
  }

  loadBudgets(): void {
    this.loading = true;
    this.financeService.getAllBudgets().subscribe({
      next: (data) => {
        this.budgets = data;
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Failed to load budgets';
        this.loading = false;
        console.error(err);
      }
    });
  }

  loadPendingRapports(): void {
    this.financeService.getPendingFinanceApprovals().subscribe({
      next: (data) => {
        this.pendingRapports = data;
      },
      error: (err) => {
        console.error('Failed to load pending rapports', err);
      }
    });
  }

  createBudget(formData: any): void {
    this.financeService.createBudget(formData).subscribe({
      next: (data) => {
        this.budgets.push(data);
        // Show success message
      },
      error: (err) => {
        this.error = 'Failed to create budget';
        console.error(err);
      }
    });
  }

  approveRapport(rapportId: number, notes: string): void {
    this.financeService.approveRapportByFinance(rapportId, notes).subscribe({
      next: (data) => {
        this.loadPendingRapports();
        // Show success message
      },
      error: (err) => {
        this.error = 'Failed to approve rapport';
        console.error(err);
      }
    });
  }

  rejectRapport(rapportId: number, notes: string): void {
    this.financeService.rejectRapportByFinance(rapportId, notes).subscribe({
      next: (data) => {
        this.loadPendingRapports();
        // Show success message
      },
      error: (err) => {
        this.error = 'Failed to reject rapport';
        console.error(err);
      }
    });
  }
}
*/
