-- V27: Link maintenance_cost_machines to machines table
-- This connects the financial view to the operational machine entity

-- Add machine_id column to maintenance_cost_machines
ALTER TABLE maintenance_cost_machines 
ADD COLUMN IF NOT EXISTS machine_id BIGINT;

-- Add foreign key constraint
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints 
        WHERE constraint_name = 'fk_maintenance_cost_machine_machine'
        AND table_name = 'maintenance_cost_machines'
    ) THEN
        ALTER TABLE maintenance_cost_machines
        ADD CONSTRAINT fk_maintenance_cost_machine_machine
        FOREIGN KEY (machine_id) REFERENCES machines(id) ON DELETE SET NULL;
    END IF;
END $$;

-- Create index for performance
CREATE INDEX IF NOT EXISTS idx_maintenance_cost_machines_machine_id 
ON maintenance_cost_machines(machine_id);

-- Add comment
COMMENT ON COLUMN maintenance_cost_machines.machine_id IS 
'Foreign key to machines table - links financial data to operational machine';
