-- Cleanup script to reset database
-- This drops and recreates the pfe database to reset Flyway migrations

-- Terminate any connections to pfe database
SELECT pg_terminate_backend(pg_stat_activity.pid)
FROM pg_stat_activity
WHERE pg_stat_activity.datname = 'pfe' AND pid <> pg_backend_pid();

-- Drop the database
DROP DATABASE IF EXISTS pfe;

-- Recreate the database
CREATE DATABASE pfe
  WITH
  ENCODING = 'UTF8'
  LC_COLLATE = 'en_US.UTF-8'
  LC_CTYPE = 'en_US.UTF-8';

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE pfe TO postgres;
