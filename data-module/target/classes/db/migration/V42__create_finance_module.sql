-- V42__create_finance_module.sql
-- Finance Module: expense_reports and annual_budgets tables

-- ============================================================================
-- EXPENSE REPORTS
-- ============================================================================
CREATE TABLE IF NOT EXISTS expense_reports (
    id                  BIGSERIAL PRIMARY KEY,
    title               VARCHAR(255)    NOT NULL,
    description         TEXT,
    amount              NUMERIC(19, 2)  NOT NULL,
    category            VARCHAR(50)     NOT NULL,
    machine_id          BIGINT,
    machine_name        VARCHAR(255),
    maintenance_task_id BIGINT,
    submitted_by        VARCHAR(100)    NOT NULL,
    submitted_by_name   VARCHAR(255),
    status              VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    reviewed_by         VARCHAR(100),
    reviewed_date       TIMESTAMP,
    review_note         TEXT,
    rejection_reason    TEXT,
    created_date        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_date  TIMESTAMP,
    version             BIGINT          NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_expense_status       ON expense_reports (status);
CREATE INDEX IF NOT EXISTS idx_expense_category     ON expense_reports (category);
CREATE INDEX IF NOT EXISTS idx_expense_submitted_by ON expense_reports (submitted_by);
CREATE INDEX IF NOT EXISTS idx_expense_machine_id   ON expense_reports (machine_id);
CREATE INDEX IF NOT EXISTS idx_expense_created_date ON expense_reports (created_date);

-- ============================================================================
-- ANNUAL BUDGETS
-- ============================================================================
CREATE TABLE IF NOT EXISTS annual_budgets (
    id                     BIGSERIAL PRIMARY KEY,
    year                   INTEGER         NOT NULL UNIQUE,
    total_budget           NUMERIC(19, 2)  NOT NULL,
    spent_amount           NUMERIC(19, 2)  NOT NULL DEFAULT 0.00,
    remaining_budget       NUMERIC(19, 2)  NOT NULL,
    utilization_percentage NUMERIC(5, 2),
    notes                  TEXT,
    created_by             VARCHAR(100),
    created_date           TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_date     TIMESTAMP,
    version                BIGINT          NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_budget_year ON annual_budgets (year);

COMMENT ON TABLE expense_reports IS 'Expense reports submitted by staff for finance manager review';
COMMENT ON TABLE annual_budgets  IS 'Annual maintenance budget tracking; updated on expense approval';
