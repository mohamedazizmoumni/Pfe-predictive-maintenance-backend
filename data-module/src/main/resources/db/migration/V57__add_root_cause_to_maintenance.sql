-- Lets a Manager record why a CORRECTIVE/EMERGENCY repair happened, feeding
-- failure/reliability analytics (see ReliabilityController) without a
-- separate RootCauseAnalysis entity/workflow — enterprise scope reorientation
-- 2026-08-01 folds Reliability Engineer responsibilities into Manager rather
-- than adding a role.
ALTER TABLE maintenance ADD COLUMN root_cause TEXT;
