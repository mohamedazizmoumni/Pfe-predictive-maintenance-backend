-- Real supplier directory, extending Stock Manager with Procurement Officer
-- responsibilities (2026-08-01 scope reorientation folds that role in rather
-- than adding a new one).
CREATE TABLE IF NOT EXISTS suppliers (
    id                  BIGSERIAL PRIMARY KEY,
    name                VARCHAR(255)  NOT NULL,
    contact_name        VARCHAR(255),
    email               VARCHAR(255),
    phone               VARCHAR(50),
    address             VARCHAR(500),
    lead_time_days      INTEGER,
    notes               VARCHAR(1000),
    active              BOOLEAN       NOT NULL DEFAULT TRUE,
    created_date        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_date  TIMESTAMP,
    version             BIGINT        NOT NULL DEFAULT 0
);

ALTER TABLE stock_orders ADD COLUMN IF NOT EXISTS supplier_id BIGINT REFERENCES suppliers (id);
CREATE INDEX IF NOT EXISTS idx_stock_orders_supplier_id ON stock_orders (supplier_id);

COMMENT ON TABLE suppliers IS 'Supplier directory managed by Stock Manager (Procurement Officer responsibilities folded in)';
