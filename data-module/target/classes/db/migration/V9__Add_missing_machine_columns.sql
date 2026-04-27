-- V9__Add_missing_machine_columns.sql
-- Add missing columns identified in entity mappings

-- Add missing columns to machines table
ALTER TABLE IF EXISTS machines 
ADD COLUMN IF NOT EXISTS name VARCHAR(255);

ALTER TABLE IF EXISTS machines 
ADD COLUMN IF NOT EXISTS description VARCHAR(500);

ALTER TABLE IF EXISTS machines 
ADD COLUMN IF NOT EXISTS installation_date TIMESTAMP;

ALTER TABLE IF EXISTS machines 
ADD COLUMN IF NOT EXISTS last_maintenance_date TIMESTAMP;

ALTER TABLE IF EXISTS machines 
ADD COLUMN IF NOT EXISTS next_maintenance_date TIMESTAMP;

ALTER TABLE IF EXISTS machines 
ADD COLUMN IF NOT EXISTS operating_hours DOUBLE PRECISION;

ALTER TABLE IF EXISTS machines 
ADD COLUMN IF NOT EXISTS risk_score DOUBLE PRECISION;

-- Add missing columns to sensors table
ALTER TABLE IF EXISTS sensors 
ADD COLUMN IF NOT EXISTS code VARCHAR(100) UNIQUE;

ALTER TABLE IF EXISTS sensors 
ADD COLUMN IF NOT EXISTS name VARCHAR(255);

ALTER TABLE IF EXISTS sensors 
ADD COLUMN IF NOT EXISTS description VARCHAR(500);

ALTER TABLE IF EXISTS sensors 
ADD COLUMN IF NOT EXISTS type VARCHAR(100);

ALTER TABLE IF EXISTS sensors 
ADD COLUMN IF NOT EXISTS min_range DOUBLE PRECISION;

ALTER TABLE IF EXISTS sensors 
ADD COLUMN IF NOT EXISTS max_range DOUBLE PRECISION;

ALTER TABLE IF EXISTS sensors 
ADD COLUMN IF NOT EXISTS status VARCHAR(50) DEFAULT 'ACTIVE';

ALTER TABLE IF EXISTS sensors 
ADD COLUMN IF NOT EXISTS last_reading_time TIMESTAMP;

ALTER TABLE IF EXISTS sensors 
ADD COLUMN IF NOT EXISTS last_reading_value DOUBLE PRECISION;

-- Add index for sensors code column if not exists
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_sensors_code') THEN
        CREATE INDEX idx_sensors_code ON sensors(code);
    END IF;
END $$;
