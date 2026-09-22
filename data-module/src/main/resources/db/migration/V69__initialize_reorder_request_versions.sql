-- Hibernate optimistic locking requires every existing reorder request to
-- have a non-null version before an update can be issued safely.
UPDATE reorder_requests
SET version = 0
WHERE version IS NULL;

ALTER TABLE reorder_requests
    ALTER COLUMN version SET DEFAULT 0,
    ALTER COLUMN version SET NOT NULL;