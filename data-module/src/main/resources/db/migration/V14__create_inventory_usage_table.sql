-- Create inventory_usage table required by InventoryUsage entity
CREATE TABLE IF NOT EXISTS inventory_usage (
    id BIGSERIAL PRIMARY KEY,
    part_id BIGINT NOT NULL,
    quantity_used INTEGER NOT NULL,
    task_id BIGINT,
    reason VARCHAR(50),
    used_by VARCHAR(100),
    notes VARCHAR(1000),
    used_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

DO $$
BEGIN
    IF to_regclass('public.parts') IS NOT NULL THEN
        IF NOT EXISTS (
            SELECT 1
            FROM information_schema.table_constraints
            WHERE constraint_name = 'fk_inventory_usage_part'
              AND table_name = 'inventory_usage'
        ) THEN
            ALTER TABLE inventory_usage
                ADD CONSTRAINT fk_inventory_usage_part
                FOREIGN KEY (part_id) REFERENCES parts(id);
        END IF;
    END IF;
END
$$;

CREATE INDEX IF NOT EXISTS idx_inventory_usage_part_id ON inventory_usage(part_id);
CREATE INDEX IF NOT EXISTS idx_inventory_usage_task_id ON inventory_usage(task_id);
CREATE INDEX IF NOT EXISTS idx_inventory_usage_used_date ON inventory_usage(used_date);
