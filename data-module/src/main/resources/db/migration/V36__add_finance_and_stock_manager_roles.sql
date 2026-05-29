-- Add FINANCE_MANAGER and STOCK_MANAGER roles
INSERT INTO roles (name, description) VALUES
('FINANCE_MANAGER', 'Finance manager with budget and rapport approval access'),
('STOCK_MANAGER', 'Stock manager with inventory management access')
ON CONFLICT (name) DO NOTHING;

-- Add permissions for finance operations
INSERT INTO permissions (name, description) VALUES
('MANAGE_BUDGETS', 'Can manage department budgets'),
('VIEW_BUDGETS', 'Can view department budgets'),
('APPROVE_RAPPORTS', 'Can approve maintenance rapports'),
('MANAGE_STOCK', 'Can manage stock items'),
('VIEW_STOCK', 'Can view stock items'),
('MANAGE_REORDERS', 'Can manage stock reorders')
ON CONFLICT (name) DO NOTHING;

-- Assign permissions to FINANCE_MANAGER role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'FINANCE_MANAGER' AND p.name IN (
    'VIEW_DASHBOARD', 'VIEW_MACHINES', 'VIEW_MAINTENANCE', 
    'VIEW_INVENTORY', 'VIEW_ALERTS', 'VIEW_REPORTS',
    'MANAGE_BUDGETS', 'VIEW_BUDGETS', 'APPROVE_RAPPORTS',
    'VIEW_STOCK', 'MANAGE_REORDERS', 'EXPORT_DATA'
)
ON CONFLICT DO NOTHING;

-- Assign permissions to STOCK_MANAGER role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'STOCK_MANAGER' AND p.name IN (
    'VIEW_DASHBOARD', 'VIEW_MACHINES', 'VIEW_MAINTENANCE',
    'MANAGE_STOCK', 'VIEW_STOCK', 'MANAGE_REORDERS',
    'VIEW_INVENTORY', 'EXPORT_DATA'
)
ON CONFLICT DO NOTHING;

-- Create a test finance manager user
INSERT INTO users (username, email, password, first_name, last_name, display_name, department, status, locked, mfa_enabled, created_date)
VALUES ('financemanager', 'financemanager@predictive-maintenance.local', 
        '$2a$10$slYQmyNdGzin7olVeY5/OPST9/PgBkqquzi8Ay0IQI7tY4xZS2Dmy', 
        'Finance', 'Manager', 'Finance Manager', 'Finance', 'ACTIVE', false, false, NOW())
ON CONFLICT (username) DO NOTHING;

-- Assign FINANCE_MANAGER role to finance manager user
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.username = 'financemanager' AND r.name = 'FINANCE_MANAGER'
ON CONFLICT DO NOTHING;

-- Create a test stock manager user
INSERT INTO users (username, email, password, first_name, last_name, display_name, department, status, locked, mfa_enabled, created_date)
VALUES ('stockmanager', 'stockmanager@predictive-maintenance.local',
        '$2a$10$slYQmyNdGzin7olVeY5/OPST9/PgBkqquzi8Ay0IQI7tY4xZS2Dmy',
        'Stock', 'Manager', 'Stock Manager', 'Operations', 'ACTIVE', false, false, NOW())
ON CONFLICT (username) DO NOTHING;

-- Assign STOCK_MANAGER role to stock manager user
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.username = 'stockmanager' AND r.name = 'STOCK_MANAGER'
ON CONFLICT DO NOTHING;
