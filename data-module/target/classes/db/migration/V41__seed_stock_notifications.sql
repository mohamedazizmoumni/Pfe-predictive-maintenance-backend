-- V41: Seed initial stock notifications for testing
-- Only inserts if machine id=1 exists to avoid FK violations on a fresh schema

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM machines WHERE id = 1) THEN
        INSERT INTO notifications (machine_id, title, body, risk_level, target_roles, is_read, created_at)
        VALUES
        (1, 'Welcome to Stock Notifications',
         'You can now receive real-time notifications about inventory levels, reorder requests, and stock orders.',
         'LOW', 'STOCK_MANAGER,MANAGER,ADMIN', false, CURRENT_TIMESTAMP),
        (1, 'Notification System Active',
         'The notification system is now configured for stock management. You will receive alerts for low stock, out of stock, and reorder updates.',
         'LOW', 'STOCK_MANAGER,MANAGER,ADMIN', false, CURRENT_TIMESTAMP);
    END IF;
END $$;

COMMENT ON TABLE notifications IS 'Stores notifications for machine predictions and stock management';
