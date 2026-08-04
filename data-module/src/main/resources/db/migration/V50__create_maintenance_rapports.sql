-- V50__create_maintenance_rapports.sql
-- Maintenance rapport workflow: technician submits -> manager approves -> finance approves
--
-- V35 created an earlier, incompatible version of maintenance_rapports/rapport_parts
-- (technician_id/manager_id FK-based) that has no matching entity and is unused (0 rows).
-- Drop it here so this migration can create the schema the MaintenanceRapport entity expects.
DROP TABLE IF EXISTS rapport_parts;
DROP TABLE IF EXISTS maintenance_rapports CASCADE;

CREATE TABLE IF NOT EXISTS maintenance_rapports (
    id                    BIGSERIAL PRIMARY KEY,
    task_id               BIGINT,
    machine_id            BIGINT,
    machine_name          VARCHAR(255),
    technician_username   VARCHAR(100)    NOT NULL,
    technician_name       VARCHAR(255),
    title                 VARCHAR(255)    NOT NULL,
    description           TEXT,
    work_performed         TEXT,
    parts_replaced        TEXT,
    labor_hours           DOUBLE PRECISION NOT NULL DEFAULT 0,
    labor_cost            NUMERIC(19, 2)  NOT NULL DEFAULT 0.00,
    parts_cost            NUMERIC(19, 2)  NOT NULL DEFAULT 0.00,
    total_cost            NUMERIC(19, 2)  NOT NULL DEFAULT 0.00,
    status                VARCHAR(30)     NOT NULL DEFAULT 'PENDING_MANAGER_APPROVAL',
    approved_by_manager   VARCHAR(100),
    manager_approved_date TIMESTAMP,
    approved_by_finance   VARCHAR(100),
    finance_approved_date TIMESTAMP,
    rejection_reason      TEXT,
    created_date          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_date    TIMESTAMP,
    version               BIGINT          NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_rapport_status       ON maintenance_rapports (status);
CREATE INDEX IF NOT EXISTS idx_rapport_technician    ON maintenance_rapports (technician_username);
CREATE INDEX IF NOT EXISTS idx_rapport_machine_id    ON maintenance_rapports (machine_id);
CREATE INDEX IF NOT EXISTS idx_rapport_created_date  ON maintenance_rapports (created_date);

CREATE TABLE IF NOT EXISTS rapport_parts (
    rapport_id  BIGINT         NOT NULL REFERENCES maintenance_rapports (id) ON DELETE CASCADE,
    part_name   VARCHAR(255)   NOT NULL,
    part_code   VARCHAR(100),
    quantity    INTEGER        NOT NULL DEFAULT 1,
    unit_cost   NUMERIC(19, 2) NOT NULL DEFAULT 0.00,
    total_cost  NUMERIC(19, 2) NOT NULL DEFAULT 0.00,
    supplier    VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_rapport_parts_rapport_id ON rapport_parts (rapport_id);

COMMENT ON TABLE maintenance_rapports IS 'Technician maintenance job rapports requiring manager then finance approval';
COMMENT ON TABLE rapport_parts        IS 'Parts consumed on a maintenance rapport';
