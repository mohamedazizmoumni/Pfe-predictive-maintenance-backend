-- The maintenance table's original date-ordering constraints (V5) assumed
-- the old workflow: Schedule -> Start -> Complete -> Approve, where a
-- manager signs off only after the work is finished, and scheduled_date was
-- treated as an earliest-start gate.
--
-- The platform now uses Accept -> Start -> Complete: an approver (a
-- Manager/Admin/Super Admin, or the assigned Technician on their own task)
-- accepts the work order BEFORE it starts, and scheduled_date is used as a
-- due date in the UI ("Due Date & Time"), not a floor on when work may
-- begin -- starting before the scheduled/due date is legitimate.

-- chk_dates required scheduled_date <= start_date, i.e. you could never
-- start a task before its due date. Drop it outright -- no ordering
-- requirement between scheduled_date and start_date holds any more.
ALTER TABLE maintenance DROP CONSTRAINT IF EXISTS chk_dates;

-- chk_approval required completed_date <= approved_date (approve after
-- completion). Replace it with the inverse: approval now happens first, so
-- require approved_date <= completed_date once both are set.
ALTER TABLE maintenance DROP CONSTRAINT IF EXISTS chk_approval;
ALTER TABLE maintenance ADD CONSTRAINT chk_approval
    CHECK (approved_date IS NULL OR completed_date IS NULL OR approved_date <= completed_date);

-- chk_completion (completed_date IS NULL OR start_date <= completed_date)
-- is untouched -- "you must start before you can complete" still holds
-- under the new workflow.
