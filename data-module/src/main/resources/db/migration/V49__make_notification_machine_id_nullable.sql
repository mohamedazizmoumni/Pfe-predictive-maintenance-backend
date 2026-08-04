-- V46: Allow notifications.machine_id to be NULL
-- Stock/inventory notifications (low stock, out of stock, AI shortage forecast,
-- reorder lifecycle) are not tied to a specific machine. The column was
-- previously NOT NULL, which caused these notifications to silently fail to
-- save (the insert was caught and swallowed by StockNotificationService).

ALTER TABLE notifications ALTER COLUMN machine_id DROP NOT NULL;

COMMENT ON COLUMN notifications.machine_id IS 'Reference to the machine that triggered the notification; NULL for stock/inventory notifications that are not machine-specific';
