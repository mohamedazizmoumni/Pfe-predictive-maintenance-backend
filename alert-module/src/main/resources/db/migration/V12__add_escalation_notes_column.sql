-- ============================================================================
-- V12__add_escalation_notes_column.sql
-- ============================================================================
-- Adds a dedicated column for storing manager escalation notes so the
-- frontend timeline can display the most recent comment without parsing the
-- main alert message body.
-- ============================================================================

ALTER TABLE alerts
    ADD COLUMN IF NOT EXISTS escalation_notes VARCHAR(1000);
