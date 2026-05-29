-- Repair Flyway migration V40
-- This script fixes the failed V40 migration by removing the failed record
-- The table and indexes already exist, so we just need to mark it as successful

-- Delete the failed migration record
DELETE FROM flyway_schema_history WHERE version = '40' AND success = false;

-- Insert successful migration record
INSERT INTO flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success)
SELECT 
    COALESCE((SELECT MAX(installed_rank) FROM flyway_schema_history), 0) + 1,
    '40',
    'create prediction history table',
    'SQL',
    'V40__create_prediction_history_table.sql',
    NULL,
    'postgres',
    NOW(),
    0,
    true
WHERE NOT EXISTS (SELECT 1 FROM flyway_schema_history WHERE version = '40' AND success = true);

-- Verify
SELECT version, description, installed_on, success 
FROM flyway_schema_history 
WHERE version IN ('37', '38', '39', '40')
ORDER BY installed_rank;
