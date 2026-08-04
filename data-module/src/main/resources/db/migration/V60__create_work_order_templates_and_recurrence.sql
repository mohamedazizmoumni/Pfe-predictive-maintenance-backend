-- Work Order Templates (reusable defaults for creating a Maintenance work
-- order) and Recurring Maintenance Rules (auto-spawn a Maintenance from a
-- template on a fixed cadence). Folds "Maintenance Planner" into Manager.

CREATE TABLE IF NOT EXISTS work_order_templates (
    id                  BIGSERIAL PRIMARY KEY,
    name                VARCHAR(150)  NOT NULL,
    description         VARCHAR(1000),
    type                VARCHAR(30)   NOT NULL,
    priority            VARCHAR(20)   NOT NULL,
    estimated_duration  INT,
    default_notes       VARCHAR(2000),
    active              BOOLEAN       NOT NULL DEFAULT TRUE,
    created_by          VARCHAR(100),
    created_date        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_date  TIMESTAMP
);

CREATE TABLE IF NOT EXISTS recurring_maintenance_rules (
    id                              BIGSERIAL PRIMARY KEY,
    machine_id                      BIGINT    NOT NULL REFERENCES machines (id) ON DELETE CASCADE,
    work_order_template_id         BIGINT    NOT NULL REFERENCES work_order_templates (id) ON DELETE CASCADE,
    interval_days                   INT       NOT NULL,
    assigned_technician_id          BIGINT,
    next_run_date                   TIMESTAMP NOT NULL,
    last_generated_maintenance_id   BIGINT,
    active                           BOOLEAN   NOT NULL DEFAULT TRUE,
    created_by                       VARCHAR(100),
    created_date                     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_date               TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_recurring_rule_machine ON recurring_maintenance_rules (machine_id);
CREATE INDEX IF NOT EXISTS idx_recurring_rule_due ON recurring_maintenance_rules (active, next_run_date);

COMMENT ON TABLE work_order_templates IS 'Reusable defaults (type/priority/description/duration) for creating a Maintenance work order';
COMMENT ON TABLE recurring_maintenance_rules IS 'Auto-spawns a Maintenance work order from a template every interval_days';
