-- Prevents duplicate open incidents for the same machine + issue type.
-- The application layer already checks-then-creates (AlertService.openIncident,
-- called from MachineStreamingService.checkAndTriggerAnomalyAlert), but that
-- check-then-act is not atomic: the 2s scheduler tick and a concurrent manual
-- trigger for the same machine can both see "no active incident" and both
-- insert one, duplicating the DB row, the /topic/alerts broadcast, and the
-- notification email. This partial unique index makes the DB itself the
-- source of truth and turns the race into a constraint violation that
-- AlertService.openIncident() catches and resolves by returning the winner.
--
-- Partial (WHERE is_active) so closed/resolved history for the same
-- machine+issueType is unrestricted, and NULL issue_type (ad-hoc alerts not
-- created through the incident model) never collides, since SQL NULLs are
-- never considered equal to each other in a unique index.
CREATE UNIQUE INDEX idx_alerts_active_incident_unique
    ON alerts (machine_id, issue_type)
    WHERE is_active = true;
