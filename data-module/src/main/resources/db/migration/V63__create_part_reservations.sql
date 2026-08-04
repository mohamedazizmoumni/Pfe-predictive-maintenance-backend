-- Reserves stock against a specific job before it's consumed. Available
-- stock = parts.current_stock - SUM(quantity_reserved) for status='RESERVED'.
CREATE TABLE IF NOT EXISTS part_reservations (
    id                 BIGSERIAL PRIMARY KEY,
    part_id            BIGINT       NOT NULL REFERENCES parts (id) ON DELETE CASCADE,
    quantity_reserved  INT          NOT NULL,
    maintenance_id     BIGINT,
    status             VARCHAR(20)  NOT NULL DEFAULT 'RESERVED',
    reserved_by        VARCHAR(100) NOT NULL,
    reserved_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at        TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_part_reservations_part ON part_reservations (part_id, status);
CREATE INDEX IF NOT EXISTS idx_part_reservations_maintenance ON part_reservations (maintenance_id);
