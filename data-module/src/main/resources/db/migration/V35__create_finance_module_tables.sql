-- V35__create_finance_module_tables.sql
-- Finance Module: Departments, Budgets, Maintenance Reports, Stock Management

-- ============================================================================
-- DEPARTMENTS TABLE
-- ============================================================================
CREATE TABLE IF NOT EXISTS departments (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(500),
    manager_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    budget_year INTEGER,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_modified_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    modified_by BIGINT REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX idx_departments_name ON departments(name);
CREATE INDEX idx_departments_status ON departments(status);
CREATE INDEX idx_departments_manager_id ON departments(manager_id);

-- ============================================================================
-- DEPARTMENT BUDGETS TABLE
-- ============================================================================
CREATE TABLE IF NOT EXISTS department_budgets (
    id BIGSERIAL PRIMARY KEY,
    department_id BIGINT NOT NULL REFERENCES departments(id) ON DELETE CASCADE,
    fiscal_year INTEGER NOT NULL,
    allocated_amount NUMERIC(19,4) NOT NULL,
    spent_amount NUMERIC(19,4) DEFAULT 0,
    remaining_amount NUMERIC(19,4),
    utilization_percentage NUMERIC(5,2) DEFAULT 0,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_modified_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    modified_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    UNIQUE(department_id, fiscal_year)
);

CREATE INDEX idx_dept_budgets_department_id ON department_budgets(department_id);
CREATE INDEX idx_dept_budgets_fiscal_year ON department_budgets(fiscal_year);
CREATE INDEX idx_dept_budgets_status ON department_budgets(status);

-- ============================================================================
-- MAINTENANCE RAPPORT TABLE (Technician Report)
-- ============================================================================
CREATE TABLE IF NOT EXISTS maintenance_rapports (
    id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    machine_id BIGINT NOT NULL REFERENCES machines(id) ON DELETE CASCADE,
    technician_id BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    work_performed TEXT NOT NULL,
    parts_replaced TEXT,
    parts_cost NUMERIC(19,4) DEFAULT 0,
    labor_hours NUMERIC(10,2) NOT NULL,
    labor_cost NUMERIC(19,4) NOT NULL,
    total_cost NUMERIC(19,4),
    status VARCHAR(20) DEFAULT 'DRAFT',
    technician_notes TEXT,
    manager_notes TEXT,
    finance_notes TEXT,
    manager_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    finance_manager_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    approved_by_manager_date TIMESTAMP,
    approved_by_finance_date TIMESTAMP,
    rejected_date TIMESTAMP,
    rejection_reason TEXT,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_modified_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_rapports_task_id ON maintenance_rapports(task_id);
CREATE INDEX idx_rapports_machine_id ON maintenance_rapports(machine_id);
CREATE INDEX idx_rapports_technician_id ON maintenance_rapports(technician_id);
CREATE INDEX idx_rapports_status ON maintenance_rapports(status);
CREATE INDEX idx_rapports_created_date ON maintenance_rapports(created_date);

-- ============================================================================
-- RAPPORT PARTS TABLE (Parts used in maintenance)
-- ============================================================================
CREATE TABLE IF NOT EXISTS rapport_parts (
    id BIGSERIAL PRIMARY KEY,
    rapport_id BIGINT NOT NULL REFERENCES maintenance_rapports(id) ON DELETE CASCADE,
    part_name VARCHAR(255) NOT NULL,
    part_code VARCHAR(100),
    quantity INTEGER NOT NULL,
    unit_cost NUMERIC(19,4) NOT NULL,
    total_cost NUMERIC(19,4),
    supplier VARCHAR(255),
    notes TEXT
);

CREATE INDEX idx_rapport_parts_rapport_id ON rapport_parts(rapport_id);

-- ============================================================================
-- STOCK MANAGEMENT TABLE
-- ============================================================================
CREATE TABLE IF NOT EXISTS stock_items (
    id BIGSERIAL PRIMARY KEY,
    part_name VARCHAR(255) NOT NULL,
    part_code VARCHAR(100) NOT NULL UNIQUE,
    category VARCHAR(100),
    quantity_in_stock INTEGER NOT NULL DEFAULT 0,
    minimum_quantity INTEGER DEFAULT 5,
    maximum_quantity INTEGER DEFAULT 100,
    unit_cost NUMERIC(19,4) NOT NULL,
    supplier VARCHAR(255),
    lead_time_days INTEGER DEFAULT 7,
    last_reorder_date TIMESTAMP,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_modified_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_stock_items_part_code ON stock_items(part_code);
CREATE INDEX idx_stock_items_category ON stock_items(category);
CREATE INDEX idx_stock_items_status ON stock_items(status);

-- ============================================================================
-- STOCK REORDER TABLE
-- ============================================================================
CREATE TABLE IF NOT EXISTS stock_reorders (
    id BIGSERIAL PRIMARY KEY,
    stock_item_id BIGINT NOT NULL REFERENCES stock_items(id) ON DELETE CASCADE,
    quantity_requested INTEGER NOT NULL,
    quantity_received INTEGER DEFAULT 0,
    status VARCHAR(20) DEFAULT 'PENDING',
    requested_by BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    approved_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    received_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    requested_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    approved_date TIMESTAMP,
    expected_delivery_date TIMESTAMP,
    received_date TIMESTAMP,
    cost_per_unit NUMERIC(19,4) NOT NULL,
    total_cost NUMERIC(19,4),
    notes TEXT
);

CREATE INDEX idx_stock_reorders_stock_item_id ON stock_reorders(stock_item_id);
CREATE INDEX idx_stock_reorders_status ON stock_reorders(status);
CREATE INDEX idx_stock_reorders_requested_date ON stock_reorders(requested_date);

-- ============================================================================
-- BUDGET TRANSACTIONS TABLE (Audit trail)
-- ============================================================================
CREATE TABLE IF NOT EXISTS budget_transactions (
    id BIGSERIAL PRIMARY KEY,
    department_budget_id BIGINT NOT NULL REFERENCES department_budgets(id) ON DELETE CASCADE,
    rapport_id BIGINT REFERENCES maintenance_rapports(id) ON DELETE SET NULL,
    transaction_type VARCHAR(50) NOT NULL,
    amount NUMERIC(19,4) NOT NULL,
    description VARCHAR(500),
    created_by BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_budget_transactions_dept_budget_id ON budget_transactions(department_budget_id);
CREATE INDEX idx_budget_transactions_rapport_id ON budget_transactions(rapport_id);
CREATE INDEX idx_budget_transactions_created_date ON budget_transactions(created_date);

-- ============================================================================
-- FINANCE AUDIT LOG TABLE
-- ============================================================================
CREATE TABLE IF NOT EXISTS finance_audit_logs (
    id BIGSERIAL PRIMARY KEY,
    entity_type VARCHAR(100) NOT NULL,
    entity_id BIGINT NOT NULL,
    action VARCHAR(50) NOT NULL,
    old_values TEXT,
    new_values TEXT,
    performed_by BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_finance_audit_logs_entity ON finance_audit_logs(entity_type, entity_id);
CREATE INDEX idx_finance_audit_logs_created_date ON finance_audit_logs(created_date);
